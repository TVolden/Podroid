package dk.lashout.podroid.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import dk.lashout.podroid.ui.util.URL_ANNOTATION_TAG
import dk.lashout.podroid.ui.util.toAnnotatedString

/**
 * Renders HTML text with clickable links and text selection.
 * When [expandable] is true, shows a "Show more / Show less" toggle capped at [maxLines].
 */
@Composable
fun HtmlText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    maxLines: Int = Int.MAX_VALUE,
    expandable: Boolean = false
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) { text.toAnnotatedString(linkColor) }
    if (annotated.text.isBlank()) return

    val uriHandler = LocalUriHandler.current
    val onClick: (Int) -> Unit = { offset ->
        annotated.getStringAnnotations(URL_ANNOTATION_TAG, offset, offset)
            .firstOrNull()?.let { uriHandler.openUri(it.item) }
    }

    if (expandable) {
        var expanded by remember { mutableStateOf(false) }
        Column(horizontalAlignment = Alignment.Start) {
            SelectionContainer {
                ClickableText(
                    text = annotated,
                    style = style,
                    maxLines = if (expanded) Int.MAX_VALUE else maxLines,
                    overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = modifier,
                    onClick = onClick
                )
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Show less" else "Show more")
            }
        }
    } else {
        SelectionContainer {
            ClickableText(
                text = annotated,
                style = style,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier,
                onClick = onClick
            )
        }
    }
}
