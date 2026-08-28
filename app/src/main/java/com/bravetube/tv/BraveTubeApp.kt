package com.bravetube.tv

import android.app.Application
import com.bravetube.tv.data.PipedApi
import com.bravetube.tv.data.Prefs
import com.bravetube.tv.data.Repository

/** Dead-simple manual DI — the app is small enough not to need a framework. */
object AppGraph {
    lateinit var prefs: Prefs
        private set
    lateinit var api: PipedApi
        private set
    lateinit var repo: Repository
        private set

    fun init(app: Application) {
        if (::repo.isInitialized) return
        prefs = Prefs(app)
        api = PipedApi(prefs)
        repo = Repository(api, prefs)
    }
}

class BraveTubeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
