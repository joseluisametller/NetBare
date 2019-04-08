package com.github.megatronking.netbare.sample

import android.app.Application
import android.util.Log
import android.content.Context
import com.github.megatronking.netbare.NetBare
import com.github.megatronking.netbare.NetBareUtils
import com.github.megatronking.netbare.ssl.JKS
import me.weishu.reflection.Reflection

class App : Application() {

    companion object {
        const val TAG = "NetBareSample"
        const val JSK_ALIAS = "NetBareSample"

        private lateinit var sInstance: App

        fun getInstance(): App {
            return sInstance
        }
    }

    private lateinit var mJKS : JKS
    private var rootCertificatePath: String? = null
    private var privateKeyPath: String? = null

    override fun onCreate() {
        super.onCreate()
        sInstance = this

        // Create default JKS
        createJKS()

        // 初始化NetBare
        NetBare.get().attachApplication(this, BuildConfig.DEBUG)
    }

    fun setRootCertificatePath(path: String?) {
        rootCertificatePath = path
    }

    fun setPrivateKeyPath(path: String?) {
        privateKeyPath = path
    }

    fun getJKS(): JKS {
        return mJKS
    }

    fun createJKS() {
        mJKS = if (rootCertificatePath == null || privateKeyPath == null) {
            Log.d(TAG, "In createJKS, using default certificate" )
            JKS(this, JSK_ALIAS, JSK_ALIAS.toCharArray(), JSK_ALIAS, JSK_ALIAS,
                    JSK_ALIAS, JSK_ALIAS, JSK_ALIAS)
        } else {
            Log.d(TAG, "In createJKS, using OUR certificate :" + rootCertificatePath + ", priv: " + privateKeyPath)
            JKS(this, JSK_ALIAS, JSK_ALIAS.toCharArray(), JSK_ALIAS, JSK_ALIAS,
                    JSK_ALIAS, JSK_ALIAS, JSK_ALIAS, rootCertificatePath, privateKeyPath)
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // On android Q, we can't access Java8EngineWrapper with reflect.
        if (NetBareUtils.isAndroidQ()) {
            Reflection.unseal(base)
        }
    }

}
