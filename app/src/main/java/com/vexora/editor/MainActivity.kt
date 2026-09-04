package com.vexora.editor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VexoraEditor() }
    }
}

@Composable
fun VexoraEditor() {
    val clips = remember { mutableStateListOf<Uri>() }
    var selected by remember { mutableStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(1f) }
    var speed by remember { mutableFloatStateOf(1f) }
    var tool by remember { mutableStateOf("Edit") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        clips.clear()
        clips.addAll(uris)
        selected = 0
    }

    val player = remember(clips.toList()) {
        ExoPlayer.Builder(androidx.compose.ui.platform.LocalContext.current).build().also { p ->
            clips.forEach { p.addMediaItem(MediaItem.fromUri(it)) }
            p.prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    Surface(color = Color(0xFF080808), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(onImport = { picker.launch("video/*") })
            Preview(player, Modifier.weight(1f).fillMaxWidth())
            Timeline(clips.size, selected, onSelect = { selected = it })
            if (tool == "Volume") {
                PropertyPanel("Volume", volume, 0f, 2f) { volume = it; player.volume = it }
            } else if (tool == "Speed") {
                PropertyPanel("Speed", speed, 0.25f, 4f) { speed = it; player.setPlaybackSpeed(it) }
            } else {
                Spacer(Modifier.height(8.dp))
            }
            Text(if (clips.isEmpty()) "Import a video to start editing" else "${clips.size} clip(s) • Clip ${selected + 1}", color = Color.LightGray, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            ToolBar(
                onPlay = { playing = !playing; if (playing) player.play() else player.pause() },
                onImport = { picker.launch("video/*") },
                onTool = { tool = it }
            )
        }
    }
}

@Composable
private fun TopBar(onImport: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Vexora", color = Color.White, fontSize = 21.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onImport) { Text("+ Media", color = Color.White) }
        Button(onClick = {}) { Text("Export") }
    }
}

@Composable
private fun Preview(player: ExoPlayer, modifier: Modifier) {
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { it.player = player }
        )
    }
}

@Composable
private fun Timeline(count: Int, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color(0xFF111111)).padding(vertical = 10.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (count == 0) {
                Box(Modifier.width(260.dp).height(72.dp).background(Color(0xFF202020)), contentAlignment = Alignment.Center) { Text("Video Track", color = Color.Gray) }
            } else {
                repeat(count) { i ->
                    Box(Modifier.width(120.dp).height(72.dp).background(if (i == selected) Color(0xFF353535) else Color(0xFF202020)).clickable { onSelect(i) }, contentAlignment = Alignment.Center) {
                        Text("Clip ${i + 1}", color = Color.White)
                    }
                }
            }
        }
        Row(Modifier.padding(top = 6.dp, start = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrackLabel("VIDEO")
            TrackLabel("AUDIO")
            TrackLabel("TEXT")
        }
    }
}

@Composable
private fun TrackLabel(text: String) { Text(text, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.background(Color(0xFF1B1B1B)).padding(horizontal = 8.dp, vertical = 4.dp)) }

@Composable
private fun PropertyPanel(title: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(title, color = Color.White, modifier = Modifier.weight(1f)); Text(String.format("%.2f", value), color = Color.Gray) }
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}

@Composable
private fun ToolBar(onPlay: () -> Unit, onImport: () -> Unit, onTool: (String) -> Unit) {
    val tools = listOf("Split", "Trim", "Text", "Music", "Speed", "Volume", "Filter")
    Row(Modifier.fillMaxWidth().height(82.dp).background(Color(0xFF101010)).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Tool("▶", "Play", onPlay)
        tools.forEach { name -> Tool("•", name) { if (name == "Speed" || name == "Volume") onTool(name) } }
        Tool("+", "Media", onImport)
    }
}

@Composable
private fun Tool(icon: String, label: String, onClick: () -> Unit) {
    Column(Modifier.width(68.dp).fillMaxHeight().clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(icon, color = Color.White, fontSize = 20.sp)
        Text(label, color = Color.LightGray, fontSize = 11.sp)
    }
}
