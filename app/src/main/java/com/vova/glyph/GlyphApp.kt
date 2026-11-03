package com.vova.glyph

import android.app.Application
import com.vova.glyph.i18n.LangController
import com.vova.glyph.ui.theme.FontController
import com.vova.glyph.ui.theme.ThemeController

class GlyphApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ThemeController.attach(this)
        FontController.attach(this)
        LangController.attach(this)
        container = AppContainer(this)
    }
}
