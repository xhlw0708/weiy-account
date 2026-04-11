package com.weiy.account.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

@Composable
fun LongPressActionPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier,
    shape: Shape,
    properties: PopupProperties = PopupProperties(focusable = true),
    shadowElevation: Dp = 10.dp,
    tonalElevation: Dp = 0.dp,
    pointerWidth: Dp = 12.dp,
    pointerHeight: Dp = 6.dp,
    pointerOverlap: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        val bottomPadding = (pointerHeight - pointerOverlap).coerceAtLeast(0.dp)

        Box(
            modifier = modifier,
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier.padding(bottom = bottomPadding),
                shape = shape,
                color = containerColor,
                shadowElevation = shadowElevation,
                tonalElevation = tonalElevation
            ) {
                content()
            }

            if (pointerWidth > 0.dp && pointerHeight > 0.dp) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(width = pointerWidth, height = pointerHeight)
                ) {
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width / 2f, size.height)
                            lineTo(size.width, 0f)
                            close()
                        },
                        color = containerColor
                    )
                }
            }
        }
    }
}

class TopAnchorCenterPopupPositionProvider(
    private val verticalSpacing: Int
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val centeredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val clampedX = centeredX.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))

        val preferredY = anchorBounds.top - popupContentSize.height - verticalSpacing
        val fallbackY = anchorBounds.bottom + verticalSpacing
        val finalY = if (preferredY >= 0) {
            preferredY
        } else {
            fallbackY.coerceAtMost(windowSize.height - popupContentSize.height)
        }.coerceAtLeast(0)

        return IntOffset(clampedX, finalY)
    }
}
