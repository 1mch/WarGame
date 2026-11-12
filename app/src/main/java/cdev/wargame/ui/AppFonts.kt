package cdev.wargame.ui

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import cdev.wargame.R

object AppFonts {
    private var baseInter: Typeface? = null
    private var regularTypeface: Typeface? = null
    private var semiBoldTypeface: Typeface? = null
    private var boldTypeface: Typeface? = null
    private var blackTypeface: Typeface? = null

    private fun getBase(context: Context): Typeface {
        baseInter?.let { return it }
        val loaded = ResourcesCompat.getFont(context, R.font.inter_variable) ?: Typeface.SANS_SERIF
        baseInter = loaded
        return loaded
    }

    fun regular(context: Context): Typeface {
        regularTypeface?.let { return it }
        return Typeface.create(getBase(context), 400, false).also { regularTypeface = it }
    }

    fun semiBold(context: Context): Typeface {
        semiBoldTypeface?.let { return it }
        return Typeface.create(getBase(context), 600, false).also { semiBoldTypeface = it }
    }

    fun bold(context: Context): Typeface {
        boldTypeface?.let { return it }
        return Typeface.create(getBase(context), 700, false).also { boldTypeface = it }
    }

    fun black(context: Context): Typeface {
        blackTypeface?.let { return it }
        return Typeface.create(getBase(context), 900, false).also { blackTypeface = it }
    }
}

