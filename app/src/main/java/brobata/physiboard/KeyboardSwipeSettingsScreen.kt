package brobata.physiboard

import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwipeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import brobata.physiboard.ui.SettingsTopBar
import kotlin.math.roundToInt

/** Preference keys this screen watches; must match SettingsManager. */
private const val KEY_TRACKPAD_GESTURES_ENABLED = "trackpad_gestures_enabled"
private const val KEY_TRACKPAD_GESTURE_ADD_WORD_ENABLED = "trackpad_gesture_add_word_enabled"
private const val KEY_TRACKPAD_SWIPE_THRESHOLD = "trackpad_swipe_threshold"
private const val KEY_TRACKPAD_SUGGESTION_SWIPE_THRESHOLD = "trackpad_suggestion_swipe_threshold"

/** Slider granularity for the swipe distance, in pixels. */
private const val SWIPE_THRESHOLD_STEP_PX = 10

/** Alpha Material 3 uses for disabled content. */
private const val DISABLED_ALPHA = 0.38f

/**
 * Keyboard swipe screen: the swipe-up-to-accept-a-suggestion gesture on the physical
 * keys. State is hoisted here and re-read when a preference changes elsewhere. The
 * provider stays at its default (native IME events); there is no provider picker.
 */
@Composable
fun KeyboardSwipeSettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var gesturesEnabled by remember {
        mutableStateOf(SettingsManager.getTrackpadGesturesEnabled(context))
    }
    var addWordEnabled by remember {
        mutableStateOf(SettingsManager.getTrackpadGestureAddWordEnabled(context))
    }
    var swipeThreshold by remember {
        mutableFloatStateOf(SettingsManager.getTrackpadSuggestionSwipeThreshold(context))
    }
    DisposableEffect(context) {
        val prefs = SettingsManager.getPreferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                KEY_TRACKPAD_GESTURES_ENABLED ->
                    gesturesEnabled = SettingsManager.getTrackpadGesturesEnabled(context)
                KEY_TRACKPAD_GESTURE_ADD_WORD_ENABLED ->
                    addWordEnabled = SettingsManager.getTrackpadGestureAddWordEnabled(context)
                // The suggestion threshold falls back to the shared one when unset.
                KEY_TRACKPAD_SWIPE_THRESHOLD, KEY_TRACKPAD_SUGGESTION_SWIPE_THRESHOLD ->
                    swipeThreshold = SettingsManager.getTrackpadSuggestionSwipeThreshold(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.trackpad_gestures_title),
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            KeyboardSwipeSwitchRow(
                icon = Icons.Filled.SwipeUp,
                title = stringResource(R.string.trackpad_gestures_enabled_title),
                description = stringResource(R.string.trackpad_gestures_enabled_description),
                checked = gesturesEnabled,
                onCheckedChange = { value ->
                    gesturesEnabled = value
                    SettingsManager.setTrackpadGesturesEnabled(context, value)
                }
            )
            KeyboardSwipeSwitchRow(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                title = stringResource(R.string.trackpad_gesture_add_word_title),
                description = stringResource(R.string.trackpad_gesture_add_word_description),
                checked = addWordEnabled,
                enabled = gesturesEnabled,
                onCheckedChange = { value ->
                    addWordEnabled = value
                    SettingsManager.setTrackpadGestureAddWordEnabled(context, value)
                }
            )
            KeyboardSwipeThresholdRow(
                threshold = swipeThreshold,
                enabled = gesturesEnabled,
                onThresholdChange = { swipeThreshold = it },
                onThresholdChangeFinished = {
                    SettingsManager.setTrackpadSuggestionSwipeThreshold(context, swipeThreshold)
                }
            )
            KeyboardSwipeNoteRow(text = stringResource(R.string.trackpad_gestures_note))
            KeyboardSwipeDiagnosticsRow(
                onClick = {
                    context.startActivity(Intent(context, TrackpadDebugActivity::class.java))
                }
            )
        }
    }
}

/** Switch row; tapping anywhere on the row toggles it. Greyed and inert when disabled. */
@Composable
private fun KeyboardSwipeSwitchRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.disabledIf(!enabled),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.disabledIf(!enabled),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.disabledIf(!enabled),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

/**
 * Slider row for the swipe distance that picks a suggestion. The value is committed
 * when the drag ends, because the IME rebuilds its gesture detector on every change.
 */
@Composable
private fun KeyboardSwipeThresholdRow(
    threshold: Float,
    enabled: Boolean,
    onThresholdChange: (Float) -> Unit,
    onThresholdChangeFinished: () -> Unit
) {
    val min = SettingsManager.getMinTrackpadSwipeThreshold()
    val max = SettingsManager.getMaxTrackpadSwipeThreshold()
    val steps = ((max - min) / SWIPE_THRESHOLD_STEP_PX).roundToInt() - 1
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Straighten,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.disabledIf(!enabled),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.trackpad_suggestion_swipe_threshold_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.disabledIf(!enabled),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.trackpad_swipe_threshold_value,
                        threshold.roundToInt()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.disabledIf(!enabled),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Slider(
                value = threshold,
                onValueChange = { newValue ->
                    val snapped = (newValue / SWIPE_THRESHOLD_STEP_PX).roundToInt() *
                        SWIPE_THRESHOLD_STEP_PX.toFloat()
                    onThresholdChange(snapped.coerceIn(min, max))
                },
                onValueChangeFinished = onThresholdChangeFinished,
                valueRange = min..max,
                steps = steps,
                enabled = enabled,
                modifier = Modifier.weight(1.5f)
            )
        }
    }
}

/** Plain, non-clickable note under the controls. */
@Composable
private fun KeyboardSwipeNoteRow(text: String) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Opens TrackpadDebugActivity, which lists the raw motion events the keyboard sends. */
@Composable
private fun KeyboardSwipeDiagnosticsRow(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.BugReport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.trackpad_gestures_diagnostics_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.trackpad_gestures_diagnostics_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun Color.disabledIf(disabled: Boolean): Color =
    if (disabled) copy(alpha = DISABLED_ALPHA) else this
