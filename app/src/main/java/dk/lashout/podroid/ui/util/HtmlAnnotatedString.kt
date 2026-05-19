package dk.lashout.podroid.ui.util

import android.text.Html
import android.text.style.URLSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

internal const val URL_ANNOTATION_TAG = "URL"

fun String.toAnnotatedString(linkColor: Color): AnnotatedString {
    val spanned = Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
    val plain = spanned.toString()
    return buildAnnotatedString {
        append(plain)
        spanned.getSpans(0, spanned.length, URLSpan::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start >= 0 && end <= plain.length && start < end) {
                addStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    start, end
                )
                addStringAnnotation(URL_ANNOTATION_TAG, span.url, start, end)
            }
        }
    }
}
