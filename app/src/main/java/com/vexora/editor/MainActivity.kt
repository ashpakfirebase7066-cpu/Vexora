package com.vexora.editor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private val Bg = Color(0xFF111216)
private val Panel = Color(0xFF17181D)
private val Panel2 = Color(0xFF202229)
private val Soft = Color(0xFF9A9CA5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VexoraEditor() }
    }
}

@Composable
fun VexoraEditor() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clips = remember { mutableStateListOf<Uri>() }
    var selected by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf("Edit") }
    var volume by remember { mutableFloatStateOf(1f) }
    var speed by remember { mutableFloatStateOf(1f) }
    var dialog by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            clips.clear()
            clips.addAll(uris)
            selected = 0
            playing = false
        }
    }

    val player = remember(clips.toList()) {
        ExoPlayer.Builder(context).build().also { p ->
            clips.forEach { p.addMediaItem(MediaItem.fromUri(it)) }
            p.prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    dialog?.let { message ->
        AlertDialog(onDismissRequest = { dialog = null }, title = { Text("Vexora") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { dialog = null }) { Text("OK") } })
    }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            TopBar({ dialog = "Back to projects" }, { picker.launch("video/*") }, { dialog = "Export options: 720p, 1080p and 4K." })
            Preview(player, playing) { playing = !playing; if (playing) player.play() else player.pause() }
            Timeline(clips.size, selected, { index -> selected = index; player.seekTo(index, 0L); player.pause(); playing = false }, { picker.launch("video/*") }) { action ->
                dialog = when (action) { "Text" -> "Text layer controls opened."; "Music" -> "Choose an audio file from your device."; "Overlay" -> "Choose an image overlay."; else -> "$action controls opened." }
            }
            if (tool == "Volume" || tool == "Speed") {
                PropertyBar(tool, if (tool == "Volume") volume else speed, if (tool == "Volume") 0f..2f else 0.25f..4f) { value ->
                    if (tool == "Volume") { volume = value; player.volume = value } else { speed = value; player.setPlaybackSpeed(value) }
                }
            }
            ToolBar(tool) { name -> tool = name; if (name != "Edit" && name != "Volume" && name != "Speed") dialog = "$name controls opened." }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onImport: () -> Unit, onExport: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp).background(Color(0xFF15161A)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable { onBack() }.padding(end = 14.dp))
        Text("Vexora", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(24.dp)); Text("Original ▾", color = Soft, fontSize = 12.sp); Spacer(Modifier.width(18.dp)); Text("⋯", color = Color.White, fontSize = 25.sp); Spacer(Modifier.width(12.dp))
        TextButton(onClick = onImport) { Text("＋", color = Color.White, fontSize = 18.sp) }
        Button(onClick = onExport, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C8CFF)), shape = RoundedCornerShape(6.dp)) { Text("Export", color = Color.White) }
    }
}

@Composable
private fun Preview(player: ExoPlayer, playing: Boolean, onPlay: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(320.dp).background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(factory = { context -> PlayerView(context).apply { this.player = player; useController = false; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); setShutterBackgroundColor(android.graphics.Color.BLACK) } }, modifier = Modifier.fillMaxSize(), update = { it.player = player })
        if (!playing) Box(Modifier.size(54.dp).background(Color(0x99000000), RoundedCornerShape(27.dp)).clickable { onPlay() }, contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 22.sp) }
    }
}

@Composable
private fun Timeline(count: Int, selected: Int, onSelect: (Int) -> Unit, onAdd: () -> Unit, onAction: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().height(190.dp).background(Color(0xFF15161A))) {
        Row(Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (count == 0) "0:00 / 0:00" else "0:00 / 0:03", color = Soft, fontSize = 11.sp); Spacer(Modifier.width(24.dp)); Text("↶", color = Soft, fontSize = 20.sp); Spacer(Modifier.width(12.dp)); Text("↷", color = Soft, fontSize = 20.sp) }
        Row(Modifier.fillMaxWidth().height(158.dp)) {
            Column(Modifier.width(150.dp).fillMaxHeight().padding(start = 10.dp)) { TrackButton("♫+", "Music") { onAction("Music") }; TrackButton("T+", "Subtitle") { onAction("Text") }; TrackButton("▧+", "Sticker / Overlay") { onAction("Overlay") }; TrackButton("▣+", "Video") { onAdd() } }
            Column(Modifier.fillMaxWidth().fillMaxHeight().padding(top = 10.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { if (count == 0) ClipBlock("Add video", false, onAdd) else repeat(count) { index -> ClipBlock("Clip ${index + 1}", index == selected) { onSelect(index) } }; Box(Modifier.width(54.dp).height(76.dp).border(1.dp, Color(0xFF34363D), RoundedCornerShape(5.dp)).clickable { onAdd() }, contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontSize = 28.sp) } }
                Box(Modifier.width(2.dp).height(84.dp).background(Color.White).align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable private fun TrackButton(icon: String, label: String, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().height(38.dp).clickable { onClick() }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, color = Color.White, fontSize = 16.sp, modifier = Modifier.width(34.dp)); Text(label, color = Soft, fontSize = 10.sp, maxLines = 1) } }

@Composable private fun ClipBlock(title: String, selected: Boolean, onClick: () -> Unit) { Column(Modifier.width(118.dp).height(76.dp).background(if (selected) Color(0xFF353840) else Panel2, RoundedCornerShape(5.dp)).border(1.dp, if (selected) Color.White else Color.Transparent, RoundedCornerShape(5.dp)).clickable { onClick() }) { Row(Modifier.fillMaxWidth().height(56.dp)) { Box(Modifier.width(39.dp).fillMaxHeight().padding(1.dp).background(Color(0xFF2A2C31))); Box(Modifier.width(39.dp).fillMaxHeight().padding(1.dp).background(Color(0xFF34363C))); Box(Modifier.width(39.dp).fillMaxHeight().padding(1.dp).background(Color(0xFF292B30))) }; Text(title, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }

@Composable private fun PropertyBar(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) { Row(Modifier.fillMaxWidth().height(54.dp).background(Panel).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, color = Color.White, modifier = Modifier.width(70.dp)); Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.width(360.dp)); Text(String.format("%.2f", value), color = Soft, fontSize = 11.sp, modifier = Modifier.padding(start = 10.dp)) } }

@Composable private fun ToolBar(selected: String, onTool: (String) -> Unit) { val tools = listOf("Filter" to "◌", "Trim" to "◈", "FX" to "☆", "Split" to "✂", "Flow" to "F", "Cutout" to "◉", "Crop" to "⌗", "Rotate" to "↻", "Mirror" to "◫", "Flip" to "▱", "Fit" to "⌑", "BG" to "▨", "Border" to "□", "Blur" to "▒", "Opacity" to "◉", "Zoom" to "↗", "TTS" to "A", "Mosaic" to "▦", "Magnifier" to "⊕", "Stories" to "▤", "Overlay Track" to "⇄", "Volume" to "🔊", "Speed" to "1x"); Row(Modifier.fillMaxWidth().height(88.dp).background(Color(0xFF101114)).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { tools.forEach { (name, icon) -> Column(Modifier.width(64.dp).height(78.dp).clickable { onTool(name) }.padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Box(Modifier.size(34.dp).background(if (name == selected) Color(0xFF2D3037) else Color.Transparent, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text(icon, color = Color.White, fontSize = 19.sp) }; Text(name, color = if (name == selected) Color.White else Soft, fontSize = 9.sp, maxLines = 2) } } } }
