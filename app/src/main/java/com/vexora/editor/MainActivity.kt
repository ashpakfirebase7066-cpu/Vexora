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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.draw.clip
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
private val Accent = Color(0xFFE7E7EA)

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
    var showTextDialog by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf<String?>(null) }

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
            clips.forEach { uri -> p.addMediaItem(MediaItem.fromUri(uri)) }
            p.prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Add Text") },
            text = { Text("Text layer controls are ready for the editor timeline.") },
            confirmButton = { TextButton(onClick = { showTextDialog = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showTextDialog = false }) { Text("Cancel") } }
        )
    }
    showMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { showMessage = null },
            title = { Text("Vexora") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { showMessage = null }) { Text("OK") } }
        )
    }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize()) {
            TopEditorBar(
                onBack = { showMessage = "Back to projects" },
                onImport = { picker.launch("video/*") },
                onExport = { showMessage = "Export panel: 720p, 1080p and 4K options will be available here." }
            )
            PreviewArea(player = player, playing = playing, onPlay = {
                playing = !playing
                if (playing) player.play() else player.pause()
            })
            TimelineEditor(
                count = clips.size,
                selected = selected,
                onSelect = {
                    selected = it
                    player.seekTo(it, 0L)
                    player.pause()
                    playing = false
                },
                onAdd = { picker.launch("video/*") },
                onMusic = { showMessage = "Music: choose an audio file from your device." },
                onText = { showTextDialog = true },
                onOverlay = { picker.launch("image/*") }
            )
            if (tool == "Volume" || tool == "Speed") {
                PropertyStrip(
                    title = tool,
                    value = if (tool == "Volume") volume else speed,
                    range = if (tool == "Volume") 0f..2f else 0.25f..4f,
                    onChange = {
                        if (tool == "Volume") {
                            volume = it
                            player.volume = it
                        } else {
                            speed = it
                            player.setPlaybackSpeed(it)
                        }
                    }
                )
            }
            EditorToolBar(selectedTool = tool, onTool = { name ->
                tool = name
                when (name) {
                    "Split" -> showMessage = if (clips.isEmpty()) "Import a video first." else "Split point selected at the playhead."
                    "Trim" -> showMessage = if (clips.isEmpty()) "Import a video first." else "Trim controls opened for Clip ${selected + 1}."
                    "Text" -> showTextDialog = true
                    "Music" -> showMessage = "Music: choose an audio file from your device."
                    else -> if (name != "Edit") showMessage = "$name controls opened."
                }
            })
        }
    }
}

@Composable
private fun TopEditorBar(onBack: () -> Unit, onImport: () -> Unit, onExport: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).background(Color(0xFF15161A)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable { onBack() }.padding(end = 14.dp))
        Text("Vexora", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("Original ▾", color = Soft, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Text("⋯", color = Color.White, fontSize = 25.sp, modifier = Modifier.padding(horizontal = 10.dp))
        TextButton(onClick = onImport) { Text("＋", color = Color.White, fontSize = 18.sp) }
        Button(onClick = onExport, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C8CFF)), shape = RoundedCornerShape(6.dp)) {
            Text("Export", color = Color.White)
        }
    }
}

@Composable
private fun PreviewArea(player: ExoPlayer, playing: Boolean, onPlay: () -> Unit) {
    Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { it.player = player }
        )
        if (!playing) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).background(Color(0x99000000)).clickable { onPlay() }, contentAlignment = Alignment.Center) {
                Text("▶", color = Color.White, fontSize = 22.sp)
            }
        }
    }
}

@Composable
private fun TimelineEditor(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onMusic: () -> Unit,
    onText: () -> Unit,
    onOverlay: () -> Unit
) {
    Column(Modifier.fillMaxWidth().height(245.dp).background(Color(0xFF15161A))) {
        Row(Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (count == 0) "0:00 / 0:00" else "0:00 / 0:03", color = Soft, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text("↶", color = Soft, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp))
            Text("↷", color = Soft, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp))
        }
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(150.dp).fillMaxHeight().padding(start = 10.dp, top = 4.dp)) {
                TrackAction("♫+", "Tap to add music", onMusic)
                TrackAction("T+", "Tap to add subtitle", onText)
                TrackAction("▧+", "Tap to add sticker / Overlay", onOverlay)
                Spacer(Modifier.height(6.dp))
                TrackAction("▣+", "Video", onAdd)
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (count == 0) TimelineClip("Add video", false, onAdd)
                    else repeat(count) { index -> TimelineClip("Clip ${index + 1}", index == selected) { onSelect(index) } }
                    Box(Modifier.width(54.dp).height(76.dp).border(1.dp, Color(0xFF34363D), RoundedCornerShape(5.dp)).clickable { onAdd() }, contentAlignment = Alignment.Center) {
                        Text("+", color = Color.White, fontSize = 30.sp)
                    }
                }
                Box(Modifier.fillMaxHeight().width(2.dp).background(Color.White).align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun TrackAction(icon: String, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(43.dp).clickable { onClick() }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = Color.White, fontSize = 17.sp, modifier = Modifier.width(32.dp))
        Text(label, color = Soft, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun TimelineClip(title: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.width(118.dp).height(76.dp).clip(RoundedCornerShape(5.dp)).background(if (selected) Color(0xFF353840) else Panel2).border(1.dp, if (selected) Color.White else Color.Transparent, RoundedCornerShape(5.dp)).clickable { onClick() }) {
        Row(Modifier.fillMaxWidth().height(58.dp)) {
            repeat(3) { Box(Modifier.weight(1f).fillMaxHeight().padding(1.dp).background(Color(0xFF2A2C31))) }
        }
        Text(title, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun PropertyStrip(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().height(54.dp).background(Panel).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, modifier = Modifier.width(70.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(String.format("%.2f", value), color = Soft, fontSize = 11.sp, modifier = Modifier.width(42.dp))
    }
}

@Composable
private fun EditorToolBar(selectedTool: String, onTool: (String) -> Unit) {
    val tools = listOf(
        "Filter" to "◌", "Trim" to "◈", "FX" to "☆", "Split" to "✂", "Flow" to "F",
        "Cutout" to "◉", "Crop" to "⌗", "Rotate" to "↻", "Mirror" to "◫", "Flip" to "▱",
        "Fit" to "⌑", "BG" to "▨", "Border" to "□", "Blur" to "▒", "Opacity" to "◉",
        "Zoom" to "↗", "TTS" to "A", "Mosaic" to "▦", "Magnifier" to "⊕", "Stories" to "▤", "Overlay Track" to "⇄"
    )
    Row(Modifier.fillMaxWidth().height(88.dp).background(Color(0xFF101114)).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        tools.forEach { (name, icon) ->
            Column(Modifier.width(64.dp).fillMaxHeight().clickable { onTool(name) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (name == selectedTool) Color(0xFF2D3037) else Color.Transparent), contentAlignment = Alignment.Center) {
                    Text(icon, color = Accent, fontSize = 20.sp)
                }
                Text(name, color = if (name == selectedTool) Color.White else Soft, fontSize = 10.sp, maxLines = 2)
            }
        }
    }
}
