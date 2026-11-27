package live.mehiz.mpvkt.ui.player.manager

import android.content.Context
import android.util.Log
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.prop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import live.mehiz.mpvkt.preferences.MobileShaderPreset
import live.mehiz.mpvkt.preferences.PorcelainPreferences
import live.mehiz.mpvkt.ui.player.TAG
import java.io.File

class ShaderPresetManager(
  private val mpv: MPV,
  private val context: Context,
  private val preferences: PorcelainPreferences,
  scope: CoroutineScope,
) {
  private val shadersDir = File(context.filesDir, "shaders")

  val preset: StateFlow<MobileShaderPreset> = preferences.shaderPreset.stateIn(scope)
  val deband: StateFlow<Boolean> = preferences.shaderDeband.stateIn(scope)

  init {
    extractBundledShaders()
    applyCurrent()
  }

  private fun extractBundledShaders() {
    runCatching {
      if (!shadersDir.exists()) shadersDir.mkdirs()
      val assetFiles = context.assets.list("shaders").orEmpty()
      for (name in assetFiles) {
        val outFile = File(shadersDir, name)
        if (!outFile.exists() || outFile.length() == 0L) {
          context.assets.open("shaders/$name").use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
          }
        }
      }
    }.onFailure { e ->
      Log.w(TAG, "Failed to extract bundled shaders: ${e.message}")
    }
  }

  fun selectPreset(newPreset: MobileShaderPreset) {
    preferences.shaderPreset.set(newPreset)
    apply(newPreset, deband.value)
  }

  fun setDeband(enabled: Boolean) {
    preferences.shaderDeband.set(enabled)
    apply(preset.value, enabled)
  }

  fun applyCurrent() {
    apply(preset.value, deband.value)
  }

  private fun apply(targetPreset: MobileShaderPreset, debandEnabled: Boolean) {
    runCatching {
      mpv.command("change-list", "glsl-shaders", "clr", "")
      for (fileName in targetPreset.shaderFiles) {
        val shaderFile = File(shadersDir, fileName)
        if (shaderFile.exists()) {
          mpv.command("change-list", "glsl-shaders", "append", shaderFile.absolutePath)
        }
      }
      mpv.prop("deband", if (debandEnabled) "yes" else "no")
    }.onFailure { e ->
      Log.w(TAG, "Failed to apply shader preset ${targetPreset.id}: ${e.message}")
    }
  }
}
