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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

private val Background = Color(0xFF0D0E11)
private val SurfaceDark = Color(0xFF15171B)
private val SurfaceLight = Color(0xFF202329)
private val Muted = Color(0xFF9699A3)
private val Accent = Color(0xFF2F8CFF)

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
    var selectedClip by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf("Edit") }
    var volume by remember { mutableFloatStateOf(1f) }
    var speed by remember { mutableFloatStateOf(1f) }
    var dialog by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            clips.clear()
            clips.addAll(uris)
            selectedClip = 0
            playing = false
        }
    }

    val player = remember(clips.toList()) {
        ExoPlayer.Builder(context).build().also { exo ->
            clips.forEach { exo.addMediaItem(MediaItem.fromUri(it)) }
            exo.prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    dialog?.let { message ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Vexora Editor") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("OK") } }
        )
    }

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(Modifier.fillMaxSize()) {
            EditorTopBar(
                onBack = { dialog = "Back to projects" },
                onImport = { picker.launch("video/*") },
                onExport = { dialog = "Export is ready for the next render module." }
            )

            PreviewPanel(
                player = player,
                playing = playing,
                onPlay = {
                    playing = !playing
                    if (playing) player.play() else player.pause()
                }
            )

            PlaybackBar(player = player, playing = playing) {
                playing = !playing
                if (playing) player.play() else player.pause()
            }

            TimelinePanel(
                clipCount = clips.size,
                selectedClip = selectedClip,
                onSelectClip = { index ->
                    selectedClip = index
                    player.seekTo(index, 0L)
                    player.pause()
                    playing = false
                },
                onAddClip = { picker.launch("video/*") },
                onAction = { action -> dialog = "$action controls opened." }
            )

            if (selectedTool == "Volume" || selectedTool == "Speed") {
                val isVolume = selectedTool == "Volume"
                PropertyPanel(
                    title = selectedTool,
                    value = if (isVolume) volume else speed,
                    range = if (isVolume) 0f..2f else 0.25f..4f
                ) { value ->
                    if (isVolume) {
                        volume = value
                        player.volume = value
                    } else {
                        speed = value
                        player.setPlaybackSpeed(value)
                    }
                }
            }

            EditorToolBar(selectedTool = selectedTool) { tool ->
                selectedTool = tool
                if (tool != "Edit" && tool != "Volume" && tool != "Speed") {
                    dialog = "$tool controls opened."
                }
            }
        }
    }
}

@Composable
private fun EditorTopBar(onBack: () -> Unit, onImport: () -> Unit, onExport: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).background(SurfaceDark).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", color = Color.White, fontSize = 36.sp, modifier = Modifier.clickable { onBack }.padding(end = 12.dp))
        Text("Vexora", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text("Original ▾", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(end = 14.dp))
        Text("＋", color = Color.White, fontSize = 23.sp, modifier = Modifier.clickable { onImport }.padding(horizontal = 10.dp))
        Button(
            onClick = onExport,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(7.dp),
            modifier = Modifier.height(38.dp)
        ) { Text("Export", color = Color.White, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun PreviewPanel(player: ExoPlayer, playing: Boolean, onPlay: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(300.dp).background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { it.player = player }
        )
        if (!playing) {
            Box(
                Modifier.size(58.dp).clip(CircleShape).background(Color(0x99000000)).clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) { Text("▶", color = Color.White, fontSize = 24.sp) }
        }
    }
}

@Composable
private fun PlaybackBar(player: ExoPlayer, playing: Boolean, onPlay: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).background(SurfaceDark).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("00:00 / 00:00", color = Color.White, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text("|◀", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 12.dp))
        Text(if (playing) "Ⅱ" else "▶", color = Color.White, fontSize = 21.sp, modifier = Modifier.clickable { onPlay() }.padding(horizontal = 12.dp))
        Text("▶|", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.weight(1f))
        Text("↶", color = Muted, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp))
        Text("↷", color = Muted, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp))
        Text("⛶", color = Color.White, fontSize = 19.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun TimelinePanel(
    clipCount: Int,
    selectedClip: Int,
    onSelectClip: (Int) -> Unit,
    onAddClip: () -> Unit,
    onAction: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth().height(205.dp).background(SurfaceDark)) {
        Row(
            Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("00:00", color = Muted, fontSize = 10.sp)
            Spacer(Modifier.weight(1f))
            Text("00:05     00:10     00:15     00:20", color = Muted, fontSize = 10.sp)
        }

        Row(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.width(102.dp).fillMaxHeight().padding(start = 8.dp)) {
                TrackButton("♫", "Music") { onAction("Music") }
                TrackButton("T", "Text") { onAction("Text") }
                TrackButton("◇", "Sticker") { onAction("Sticker") }
                TrackButton("▣", "Video") { onAddClip() }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp, end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (clipCount == 0) {
                    EmptyClip(onAddClip)
                } else {
                    repeat(clipCount) { index ->
                        TimelineClip(
                            label = "Clip ${index + 1}",
                            selected = index == selectedClip,
                            onClick = { onSelectClip(index) }
                        )
                    }
                }
                AddClip(onAddClip)
            }
        }
    }
}

@Composable
private fun TrackButton(icon: String, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(40.dp).clickable { onClick() }.padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = Color.White, fontSize = 18.sp, modifier = Modifier.width(30.dp))
        Text(label, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun TimelineClip(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(118.dp).height(78.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (selected) SurfaceLight else Color(0xFF191B20))
            .border(1.dp, if (selected) Color.White else Color(0xFF2A2D34), RoundedCornerShape(5.dp))
            .clickable { onClick() }
    ) {
        Row(Modifier.fillMaxWidth().height(57.dp)) {
            repeat(3) { Box(Modifier.weight(1f).fillMaxHeight().padding(1.dp).background(Color(0xFF30333A))) }
        }
        Text(label, color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun EmptyClip(onClick: () -> Unit) {
    Box(
        Modifier.width(150.dp).height(78.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFF343740), RoundedCornerShape(6.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text("＋  Add video", color = Muted, fontSize = 12.sp) }
}

@Composable
private fun AddClip(onClick: () -> Unit) {
    Box(
        Modifier.width(52.dp).height(78.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1B1D22)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text("+", color = Color.White, fontSize = 28.sp) }
}

@Composable
private fun PropertyPanel(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).background(Color(0xFF111216)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(62.dp))
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(String.format("%.2f", value), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun EditorToolBar(selectedTool: String, onTool: (String) -> Unit) {
    val tools = listOf(
        "Trim" to "✂", "Split" to "Ⅱ", "Speed" to "1×", "Volume" to "◖",
        "Filter" to "◌", "FX" to "✦", "Text" to "T", "Sticker" to "◇",
        "Crop" to "⌗", "Rotate" to "↻", "Mirror" to "◫", "Flip" to "▱",
        "BG" to "▧", "Blur" to "▒", "Opacity" to "◉", "Zoom" to "↗",
        "TTS" to "A", "Mosaic" to "▦"
    )

    Row(
        Modifier.fillMaxWidth().height(92.dp).background(Background).horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tools.forEach { (name, icon) ->
            Column(
                Modifier.width(64.dp).fillMaxHeight().clickable { onTool(name) }.padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(if (name == selectedTool) Color(0xFF292C33) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) { Text(icon, color = Color.White, fontSize = 19.sp) }
                Text(name, color = if (name == selectedTool) Color.White else Muted, fontSize = 9.sp, maxLines = 2)
            }
        }
    }
}
