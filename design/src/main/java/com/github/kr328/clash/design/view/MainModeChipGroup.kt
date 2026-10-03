package com.github.kr328.clash.design.view

import android.content.Context
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import androidx.annotation.AttrRes
import com.github.kr328.clash.design.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.chip.ChipGroup
import kotlin.math.ceil

class MainModeChipGroup @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.chipGroupStyle
) : ChipGroup(context, attributeSet, defStyleAttr) {
    private val measuringPaint = TextPaint()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val chips = (0 until childCount).mapNotNull { index ->
            (getChildAt(index) as? Chip)?.takeIf { it.visibility != GONE }
        }

        if (chips.isNotEmpty()) {
            val horizontalPadding = resources.getDimension(R.dimen.main_mode_chip_horizontal_padding)
            val minTextSize = resources.getDimension(R.dimen.main_mode_chip_min_text_size)
            val maxTextSize = resources.getDimension(R.dimen.main_mode_chip_max_text_size)
            val minTouchSize = ceil(resources.getDimension(R.dimen.main_mode_chip_min_touch_size)).toInt()
            val margins = chips.sumOf {
                val params = it.layoutParams as ViewGroup.MarginLayoutParams
                params.marginStart + params.marginEnd
            }
            val availableWidth = (MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd -
                chipSpacingHorizontal * (chips.size - 1) - margins).coerceAtLeast(0)
            val widths = IntArray(chips.size) { index ->
                availableWidth / chips.size + if (index < availableWidth % chips.size) 1 else 0
            }

            fun fits(textSize: Float): Boolean = chips.indices.all { index ->
                textWidth(chips[index], textSize) + horizontalPadding * 2 <= widths[index]
            }

            // Wrap when readable labels or the minimum touch targets cannot fit in one row.
            val fitsOneRow = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED &&
                widths.all { it >= minTouchSize } && fits(minTextSize)
            var textSize = maxTextSize
            if (fitsOneRow && !fits(maxTextSize)) {
                var lower = minTextSize
                var upper = maxTextSize
                repeat(12) {
                    val middle = (lower + upper) / 2
                    if (fits(middle)) lower = middle else upper = middle
                }
                textSize = lower
            }

            // Recalculate from the preferred size on every measure so a wider window restores it.
            isSingleLine = fitsOneRow
            chips.forEachIndexed { index, chip ->
                if (chip.textSize != textSize) {
                    // Keep the drawable's text appearance and the TextView paint in sync.
                    (chip.chipDrawable as? ChipDrawable)?.setTextSize(textSize)
                    chip.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                }

                chip.layoutParams.width = if (fitsOneRow) {
                    widths[index]
                } else {
                    ViewGroup.LayoutParams.WRAP_CONTENT
                }
                val padding = if (fitsOneRow) {
                    ((widths[index] - textWidth(chip, textSize)) / 2).coerceAtLeast(horizontalPadding)
                } else {
                    horizontalPadding
                }
                chip.chipStartPadding = padding
                chip.chipEndPadding = padding
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun textWidth(chip: Chip, textSize: Float): Float {
        measuringPaint.set(chip.paint)
        measuringPaint.textSize = textSize
        val text = chip.transformationMethod?.getTransformation(chip.text, chip) ?: chip.text
        return measuringPaint.measureText(text.toString())
    }
}
