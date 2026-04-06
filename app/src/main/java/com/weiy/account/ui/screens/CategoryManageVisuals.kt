package com.weiy.account.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.weiy.account.model.TransactionType

internal val CategoryManageItemCircleColor = Color(0xFFF3F3F3)

private val CategoryIconTint = Color(0xFF6C6C6C)

@Composable
internal fun CategoryIconSymbol(
    name: String,
    type: TransactionType,
    iconKey: String?,
    modifier: Modifier = Modifier
) {
    val illustration = resolveCategoryIllustration(name = name, type = type, iconKey = iconKey)
    if (illustration == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = name.take(1),
                color = CategoryIconTint,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Canvas(modifier = modifier) {
            drawCategoryIllustration(illustration)
        }
    }
}

@Composable
internal fun AddCategoryIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawIconLine(0.5f, 0.24f, 0.5f, 0.76f)
        drawIconLine(0.24f, 0.5f, 0.76f, 0.5f)
    }
}

private enum class CategoryIllustration {
    UTENSILS,
    BAG,
    BRIEFCASE,
    TRANSPORT,
    FRUIT,
    SNACK,
    SPORT,
    GAME,
    PHONE,
    SHIRT,
    SPARKLE,
    HOME,
    SOFA,
    FAMILY,
    TRAVEL,
    GLASS,
    DEVICE,
    MEDICAL,
    BOOK,
    CAP,
    PAW,
    MONEY,
    GIFT,
    BOX,
    SETTINGS,
    SALARY,
    CLOCK_MONEY,
    MONEY_BAG,
    RECYCLE,
    BONUS
}

private fun resolveCategoryIllustration(
    name: String,
    type: TransactionType,
    iconKey: String?
): CategoryIllustration? {
    return when (iconKey) {
        "utensils" -> CategoryIllustration.UTENSILS
        "bag" -> CategoryIllustration.BAG
        "briefcase" -> CategoryIllustration.BRIEFCASE
        "transport" -> CategoryIllustration.TRANSPORT
        "produce", "fruit" -> CategoryIllustration.FRUIT
        "snack" -> CategoryIllustration.SNACK
        "sport" -> CategoryIllustration.SPORT
        "game" -> CategoryIllustration.GAME
        "phone" -> CategoryIllustration.PHONE
        "shirt" -> CategoryIllustration.SHIRT
        "sparkle" -> CategoryIllustration.SPARKLE
        "home" -> CategoryIllustration.HOME
        "sofa" -> CategoryIllustration.SOFA
        "family" -> CategoryIllustration.FAMILY
        "travel" -> CategoryIllustration.TRAVEL
        "glass" -> CategoryIllustration.GLASS
        "device" -> CategoryIllustration.DEVICE
        "medical" -> CategoryIllustration.MEDICAL
        "book" -> CategoryIllustration.BOOK
        "cap" -> CategoryIllustration.CAP
        "paw" -> CategoryIllustration.PAW
        "money" -> CategoryIllustration.MONEY
        "gift" -> CategoryIllustration.GIFT
        "box" -> CategoryIllustration.BOX
        "settings" -> CategoryIllustration.SETTINGS
        "salary" -> CategoryIllustration.SALARY
        "clock_money" -> CategoryIllustration.CLOCK_MONEY
        "money_bag" -> CategoryIllustration.MONEY_BAG
        "recycle" -> CategoryIllustration.RECYCLE
        "bonus" -> CategoryIllustration.BONUS
        else -> when (name) {
            "餐饮" -> CategoryIllustration.UTENSILS
            "购物" -> CategoryIllustration.BAG
            "日用", "办公" -> CategoryIllustration.BRIEFCASE
            "交通" -> CategoryIllustration.TRANSPORT
            "蔬菜", "水果" -> CategoryIllustration.FRUIT
            "零食" -> CategoryIllustration.SNACK
            "运动" -> CategoryIllustration.SPORT
            "娱乐" -> CategoryIllustration.GAME
            "通讯" -> CategoryIllustration.PHONE
            "服饰" -> CategoryIllustration.SHIRT
            "美容" -> CategoryIllustration.SPARKLE
            "住房" -> CategoryIllustration.HOME
            "居家" -> CategoryIllustration.SOFA
            "长辈", "亲友" -> CategoryIllustration.FAMILY
            "旅行" -> CategoryIllustration.TRAVEL
            "烟酒" -> CategoryIllustration.GLASS
            "数码" -> CategoryIllustration.DEVICE
            "医疗" -> CategoryIllustration.MEDICAL
            "书籍" -> CategoryIllustration.BOOK
            "学习" -> CategoryIllustration.CAP
            "宠物" -> CategoryIllustration.PAW
            "礼金" -> CategoryIllustration.MONEY
            "礼物" -> CategoryIllustration.GIFT
            "快递" -> CategoryIllustration.BOX
            "设置" -> CategoryIllustration.SETTINGS
            "工资" -> CategoryIllustration.SALARY
            "兼职" -> CategoryIllustration.CLOCK_MONEY
            "其他" -> CategoryIllustration.MONEY_BAG
            "回收" -> CategoryIllustration.RECYCLE
            "补贴", "奖金" -> CategoryIllustration.BONUS
            else -> if (type == TransactionType.INCOME) CategoryIllustration.MONEY_BAG else null
        }
    }
}

private fun DrawScope.drawCategoryIllustration(illustration: CategoryIllustration) {
    when (illustration) {
        CategoryIllustration.UTENSILS -> {
            drawIconLine(0.34f, 0.2f, 0.34f, 0.78f)
            drawIconLine(0.24f, 0.2f, 0.24f, 0.42f)
            drawIconLine(0.44f, 0.2f, 0.44f, 0.42f)
            drawIconLine(0.24f, 0.32f, 0.44f, 0.32f)
            drawIconLine(0.68f, 0.2f, 0.74f, 0.42f)
            drawIconLine(0.74f, 0.42f, 0.68f, 0.78f)
        }

        CategoryIllustration.BAG -> {
            drawIconRoundRect(0.2f, 0.3f, 0.8f, 0.78f, 0.12f)
            drawIconArc(0.33f, 0.14f, 0.67f, 0.48f, 200f, 140f)
        }

        CategoryIllustration.BRIEFCASE -> {
            drawIconRoundRect(0.18f, 0.34f, 0.82f, 0.76f, 0.1f)
            drawIconLine(0.42f, 0.26f, 0.58f, 0.26f)
            drawIconLine(0.38f, 0.34f, 0.38f, 0.26f)
            drawIconLine(0.62f, 0.34f, 0.62f, 0.26f)
            drawIconLine(0.18f, 0.52f, 0.82f, 0.52f)
        }

        CategoryIllustration.TRANSPORT -> {
            drawIconRoundRect(0.18f, 0.3f, 0.82f, 0.66f, 0.12f)
            drawIconLine(0.3f, 0.66f, 0.24f, 0.78f)
            drawIconLine(0.7f, 0.66f, 0.76f, 0.78f)
            drawIconCircle(0.32f, 0.76f, 0.08f)
            drawIconCircle(0.68f, 0.76f, 0.08f)
            drawIconLine(0.28f, 0.42f, 0.72f, 0.42f)
        }

        CategoryIllustration.FRUIT -> {
            drawIconCircle(0.5f, 0.56f, 0.22f)
            drawIconLine(0.5f, 0.2f, 0.5f, 0.34f)
            drawIconArc(0.46f, 0.16f, 0.7f, 0.34f, 200f, 140f)
        }

        CategoryIllustration.SNACK -> {
            drawIconLine(0.32f, 0.26f, 0.68f, 0.26f)
            drawIconLine(0.26f, 0.46f, 0.74f, 0.46f)
            drawIconLine(0.3f, 0.64f, 0.7f, 0.64f)
            drawIconPath(
                moveTo = 0.24f to 0.46f,
                lines = listOf(
                    0.3f to 0.76f,
                    0.7f to 0.76f,
                    0.76f to 0.46f
                ),
                closePath = true
            )
        }

        CategoryIllustration.SPORT -> {
            drawIconCircle(0.28f, 0.68f, 0.12f)
            drawIconCircle(0.72f, 0.68f, 0.12f)
            drawIconLine(0.28f, 0.68f, 0.46f, 0.44f)
            drawIconLine(0.46f, 0.44f, 0.62f, 0.44f)
            drawIconLine(0.62f, 0.44f, 0.72f, 0.68f)
            drawIconLine(0.46f, 0.44f, 0.56f, 0.68f)
            drawIconLine(0.56f, 0.68f, 0.28f, 0.68f)
        }

        CategoryIllustration.GAME -> {
            drawIconRoundRect(0.18f, 0.4f, 0.82f, 0.7f, 0.2f)
            drawIconLine(0.34f, 0.54f, 0.46f, 0.54f)
            drawIconLine(0.4f, 0.48f, 0.4f, 0.6f)
            drawIconCircle(0.62f, 0.52f, 0.04f)
            drawIconCircle(0.7f, 0.6f, 0.04f)
        }

        CategoryIllustration.PHONE -> {
            drawIconRoundRect(0.34f, 0.18f, 0.66f, 0.82f, 0.14f)
            drawIconLine(0.44f, 0.28f, 0.56f, 0.28f)
            drawIconCircle(0.5f, 0.72f, 0.03f)
        }

        CategoryIllustration.SHIRT -> {
            drawIconPath(
                moveTo = 0.2f to 0.34f,
                lines = listOf(
                    0.36f to 0.2f,
                    0.45f to 0.32f,
                    0.55f to 0.32f,
                    0.64f to 0.2f,
                    0.8f to 0.34f,
                    0.68f to 0.48f,
                    0.68f to 0.8f,
                    0.32f to 0.8f,
                    0.32f to 0.48f
                ),
                closePath = true
            )
        }

        CategoryIllustration.SPARKLE -> {
            drawIconLine(0.5f, 0.18f, 0.5f, 0.82f)
            drawIconLine(0.18f, 0.5f, 0.82f, 0.5f)
            drawIconLine(0.28f, 0.28f, 0.72f, 0.72f)
            drawIconLine(0.28f, 0.72f, 0.72f, 0.28f)
        }

        CategoryIllustration.HOME -> {
            drawIconPath(
                moveTo = 0.18f to 0.46f,
                lines = listOf(
                    0.5f to 0.2f,
                    0.82f to 0.46f
                )
            )
            drawIconRoundRect(0.28f, 0.46f, 0.72f, 0.8f, 0.06f)
            drawIconLine(0.46f, 0.8f, 0.46f, 0.6f)
            drawIconLine(0.54f, 0.8f, 0.54f, 0.6f)
        }

        CategoryIllustration.SOFA -> {
            drawIconRoundRect(0.18f, 0.44f, 0.82f, 0.72f, 0.12f)
            drawIconRoundRect(0.28f, 0.32f, 0.72f, 0.56f, 0.12f)
            drawIconLine(0.26f, 0.72f, 0.22f, 0.82f)
            drawIconLine(0.74f, 0.72f, 0.78f, 0.82f)
        }

        CategoryIllustration.FAMILY -> {
            drawIconCircle(0.38f, 0.34f, 0.12f)
            drawIconCircle(0.66f, 0.38f, 0.1f)
            drawIconArc(0.18f, 0.44f, 0.58f, 0.82f, 180f, 180f)
            drawIconArc(0.46f, 0.5f, 0.82f, 0.82f, 180f, 180f)
        }

        CategoryIllustration.TRAVEL -> {
            drawIconLine(0.18f, 0.58f, 0.82f, 0.42f)
            drawIconLine(0.42f, 0.5f, 0.64f, 0.22f)
            drawIconLine(0.44f, 0.5f, 0.74f, 0.72f)
            drawIconLine(0.34f, 0.62f, 0.28f, 0.8f)
        }

        CategoryIllustration.GLASS -> {
            drawIconPath(
                moveTo = 0.26f to 0.22f,
                lines = listOf(
                    0.74f to 0.22f,
                    0.62f to 0.52f,
                    0.38f to 0.52f
                ),
                closePath = true
            )
            drawIconLine(0.5f, 0.52f, 0.5f, 0.72f)
            drawIconLine(0.36f, 0.78f, 0.64f, 0.78f)
        }

        CategoryIllustration.DEVICE -> {
            drawIconRoundRect(0.22f, 0.24f, 0.6f, 0.8f, 0.08f)
            drawIconRoundRect(0.62f, 0.38f, 0.8f, 0.68f, 0.08f)
            drawIconCircle(0.41f, 0.7f, 0.03f)
        }

        CategoryIllustration.MEDICAL -> {
            drawIconRoundRect(0.22f, 0.22f, 0.78f, 0.78f, 0.08f)
            drawIconLine(0.5f, 0.34f, 0.5f, 0.66f)
            drawIconLine(0.34f, 0.5f, 0.66f, 0.5f)
        }

        CategoryIllustration.BOOK -> {
            drawIconPath(
                moveTo = 0.2f to 0.28f,
                lines = listOf(
                    0.46f to 0.22f,
                    0.46f to 0.78f,
                    0.2f to 0.72f
                ),
                closePath = true
            )
            drawIconPath(
                moveTo = 0.54f to 0.22f,
                lines = listOf(
                    0.8f to 0.28f,
                    0.8f to 0.72f,
                    0.54f to 0.78f
                ),
                closePath = true
            )
        }

        CategoryIllustration.CAP -> {
            drawIconPath(
                moveTo = 0.18f to 0.38f,
                lines = listOf(
                    0.5f to 0.2f,
                    0.82f to 0.38f,
                    0.5f to 0.56f
                ),
                closePath = true
            )
            drawIconLine(0.5f, 0.56f, 0.5f, 0.76f)
            drawIconLine(0.66f, 0.48f, 0.74f, 0.68f)
        }

        CategoryIllustration.PAW -> {
            drawIconCircle(0.36f, 0.32f, 0.07f)
            drawIconCircle(0.5f, 0.24f, 0.07f)
            drawIconCircle(0.64f, 0.32f, 0.07f)
            drawIconCircle(0.5f, 0.58f, 0.16f)
        }

        CategoryIllustration.MONEY -> {
            drawIconCircle(0.5f, 0.5f, 0.28f)
            drawIconLine(0.5f, 0.34f, 0.5f, 0.66f)
            drawIconLine(0.38f, 0.4f, 0.58f, 0.4f)
            drawIconLine(0.42f, 0.58f, 0.62f, 0.58f)
        }

        CategoryIllustration.GIFT -> {
            drawIconRoundRect(0.2f, 0.4f, 0.8f, 0.78f, 0.08f)
            drawIconRoundRect(0.2f, 0.26f, 0.8f, 0.44f, 0.08f)
            drawIconLine(0.5f, 0.26f, 0.5f, 0.78f)
            drawIconLine(0.2f, 0.52f, 0.8f, 0.52f)
            drawIconArc(0.32f, 0.14f, 0.5f, 0.32f, 180f, 180f)
            drawIconArc(0.5f, 0.14f, 0.68f, 0.32f, 180f, 180f)
        }

        CategoryIllustration.BOX -> {
            drawIconRoundRect(0.24f, 0.28f, 0.76f, 0.76f, 0.06f)
            drawIconLine(0.24f, 0.46f, 0.76f, 0.46f)
            drawIconLine(0.5f, 0.28f, 0.5f, 0.76f)
        }

        CategoryIllustration.SETTINGS -> {
            drawIconCircle(0.5f, 0.5f, 0.26f)
            drawIconCircle(0.5f, 0.5f, 0.09f)
            drawIconLine(0.5f, 0.14f, 0.5f, 0.24f)
            drawIconLine(0.5f, 0.76f, 0.5f, 0.86f)
            drawIconLine(0.14f, 0.5f, 0.24f, 0.5f)
            drawIconLine(0.76f, 0.5f, 0.86f, 0.5f)
        }

        CategoryIllustration.SALARY -> {
            drawIconRoundRect(0.18f, 0.28f, 0.82f, 0.72f, 0.08f)
            drawIconLine(0.32f, 0.44f, 0.68f, 0.44f)
            drawIconLine(0.32f, 0.56f, 0.68f, 0.56f)
        }

        CategoryIllustration.CLOCK_MONEY -> {
            drawIconCircle(0.42f, 0.48f, 0.22f)
            drawIconLine(0.42f, 0.48f, 0.42f, 0.34f)
            drawIconLine(0.42f, 0.48f, 0.54f, 0.56f)
            drawIconCircle(0.72f, 0.66f, 0.12f)
            drawIconLine(0.72f, 0.6f, 0.72f, 0.72f)
            drawIconLine(0.66f, 0.66f, 0.78f, 0.66f)
        }

        CategoryIllustration.MONEY_BAG -> {
            drawIconPath(
                moveTo = 0.36f to 0.2f,
                lines = listOf(
                    0.64f to 0.2f,
                    0.56f to 0.34f,
                    0.68f to 0.42f,
                    0.72f to 0.58f,
                    0.64f to 0.78f,
                    0.36f to 0.78f,
                    0.28f to 0.58f,
                    0.32f to 0.42f,
                    0.44f to 0.34f
                ),
                closePath = true
            )
            drawIconLine(0.4f, 0.2f, 0.6f, 0.2f)
        }

        CategoryIllustration.RECYCLE -> {
            drawIconArc(0.18f, 0.22f, 0.82f, 0.86f, 40f, 240f)
            drawIconLine(0.7f, 0.26f, 0.82f, 0.26f)
            drawIconLine(0.82f, 0.26f, 0.76f, 0.38f)
        }

        CategoryIllustration.BONUS -> {
            drawIconCircle(0.5f, 0.5f, 0.26f)
            drawIconLine(0.5f, 0.3f, 0.5f, 0.7f)
            drawIconLine(0.34f, 0.44f, 0.66f, 0.44f)
            drawIconLine(0.34f, 0.6f, 0.66f, 0.6f)
        }
    }
}

private fun DrawScope.drawIconLine(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float
) {
    drawLine(
        color = CategoryIconTint,
        start = point(startX, startY),
        end = point(endX, endY),
        strokeWidth = iconStrokeWidth(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawIconCircle(
    centerX: Float,
    centerY: Float,
    radiusFraction: Float
) {
    drawCircle(
        color = CategoryIconTint,
        center = point(centerX, centerY),
        radius = size.minDimension * radiusFraction,
        style = iconStroke()
    )
}

private fun DrawScope.drawIconRoundRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radiusFraction: Float
) {
    drawRoundRect(
        color = CategoryIconTint,
        topLeft = point(left, top),
        size = Size(size.width * (right - left), size.height * (bottom - top)),
        cornerRadius = CornerRadius(
            x = size.minDimension * radiusFraction,
            y = size.minDimension * radiusFraction
        ),
        style = iconStroke()
    )
}

private fun DrawScope.drawIconArc(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    startAngle: Float,
    sweepAngle: Float
) {
    drawArc(
        color = CategoryIconTint,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = point(left, top),
        size = Size(size.width * (right - left), size.height * (bottom - top)),
        style = iconStroke()
    )
}

private fun DrawScope.drawIconPath(
    moveTo: Pair<Float, Float>,
    lines: List<Pair<Float, Float>>,
    closePath: Boolean = false
) {
    val path = Path().apply {
        moveTo(point(moveTo.first, moveTo.second).x, point(moveTo.first, moveTo.second).y)
        lines.forEach { (x, y) ->
            lineTo(point(x, y).x, point(x, y).y)
        }
        if (closePath) {
            close()
        }
    }
    drawPath(
        path = path,
        color = CategoryIconTint,
        style = iconStroke()
    )
}

private fun DrawScope.iconStrokeWidth(): Float = size.minDimension * 0.08f

private fun DrawScope.iconStroke(): Stroke {
    return Stroke(
        width = iconStrokeWidth(),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
}

private fun DrawScope.point(x: Float, y: Float): Offset {
    return Offset(size.width * x, size.height * y)
}
