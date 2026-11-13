package cdev.wargame.ui

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import cdev.wargame.R

object AppFonts {
    private var regularTypeface: Typeface? = null
    private var semiBoldTypeface: Typeface? = null
    private var boldTypeface: Typeface? = null
    private var blackTypeface: Typeface? = null

    fun regular(context: Context): Typeface {
        regularTypeface?.let { return it }
        return (ResourcesCompat.getFont(context, R.font.inter_regular) ?: Typeface.SANS_SERIF).also {
            regularTypeface = it
        }
    }

    fun semiBold(context: Context): Typeface {
        semiBoldTypeface?.let { return it }
        return (ResourcesCompat.getFont(context, R.font.inter_semibold) ?: Typeface.SANS_SERIF).also {
            semiBoldTypeface = it
        }
    }

    fun bold(context: Context): Typeface {
        boldTypeface?.let { return it }
        return (ResourcesCompat.getFont(context, R.font.inter_bold) ?: Typeface.DEFAULT_BOLD).also {
            boldTypeface = it
        }
    }

    fun black(context: Context): Typeface {
        blackTypeface?.let { return it }
        return (ResourcesCompat.getFont(context, R.font.inter_black) ?: Typeface.DEFAULT_BOLD).also {
            blackTypeface = it
        }
    }
}
