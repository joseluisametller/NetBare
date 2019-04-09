package com.github.megatronking.netbare.sample

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import com.github.megatronking.netbare.NetBare
import com.github.megatronking.netbare.NetBareConfig
import com.github.megatronking.netbare.NetBareListener
import com.github.megatronking.netbare.http.HttpInjectInterceptor
import com.github.megatronking.netbare.http.HttpInterceptorFactory
import com.github.megatronking.netbare.ssl.JKS
import java.io.IOException

class MainActivity : AppCompatActivity(), NetBareListener {

    companion object {
        private const val REQUEST_CODE_PREPARE = 1
        private const val REQUEST_CODE_PICK_ROOT_CERTIFICATE = 1111
        private const val REQUEST_CODE_PICK_PRIVATE_KEY = 2222
        private const val REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_CODE = 101
        const val TAG = "NetBareSample"
        const val JSK_ALIAS = "NetBareSample"
    }

    private lateinit var mNetBare : NetBare

    private lateinit var mActionButton : Button
    private lateinit var mRootCertButton : Button
    private lateinit var mPrivateKeyButton : Button
    private lateinit var mUseRandomCertificatesRadio : RadioButton
    private lateinit var mProvideCertificatesRadio : RadioButton
    private var mRootFilePath : String = ""
    private var mPrivateKeyFilePath : String = ""

    private lateinit var mJKS : JKS
    private var rootCertificatePath: String? = null
    private var privateKeyPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mNetBare = NetBare.get()

        // Create default JKS
        createJKS()

        // 初始化NetBare
        mNetBare.attachApplication(this, BuildConfig.DEBUG)

        mActionButton = findViewById(R.id.action)
        mActionButton.setOnClickListener {
            if (mNetBare.isActive) {
                mNetBare.stop()
            } else{
                prepareNetBare()
            }
        }
        mRootCertButton = findViewById(R.id.root_certificate_button)
        mRootCertButton.setOnClickListener {
            // Open file picker and store the file path
            val mimetypes = arrayOf("application/x-pem-file", "application/x-x509-ca-cert")
            val intent = Intent()
                    .setType("*/*")
                    .setAction(Intent.ACTION_GET_CONTENT)
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes)

            startActivityForResult(Intent.createChooser(intent, getString(R.string.picker_root_certificate)), REQUEST_CODE_PICK_ROOT_CERTIFICATE)
        }
        mPrivateKeyButton = findViewById(R.id.private_key_button)
        mPrivateKeyButton.setOnClickListener {
            // Open file picker and store the file path
            val intent = Intent()
                    .setType("application/x-pem-file")
                    .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, getString(R.string.picker_private_key)), REQUEST_CODE_PICK_PRIVATE_KEY)
        }
        mUseRandomCertificatesRadio = findViewById(R.id.radioButton)
        mUseRandomCertificatesRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                privateKeyPath = null
                mPrivateKeyFilePath = ""
                rootCertificatePath = null
                mRootFilePath = ""
                createJKS()
                mActionButton.isEnabled = true
            }
        }
        mProvideCertificatesRadio = findViewById(R.id.radioButton2)
        mProvideCertificatesRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isReadExternalPermissionGranted()) {
                makePermissionRequest()
            } else {
                mRootCertButton.isEnabled = isChecked
                mPrivateKeyButton.isEnabled = isChecked
                if (isChecked) {
                    mActionButton.isEnabled = false
                }
            }
        }

        // 监听NetBare服务的启动和停止
        mNetBare.registerNetBareListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mNetBare.unregisterNetBareListener(this)
        mNetBare.stop()
    }

    override fun onServiceStarted() {
        runOnUiThread {
            mActionButton.setText(R.string.stop_netbare)
            mRootCertButton.isEnabled = false
            mPrivateKeyButton.isEnabled = false
        }
        mUseRandomCertificatesRadio.isEnabled = false
        mProvideCertificatesRadio.isEnabled = false
    }

    override fun onServiceStopped() {
        runOnUiThread {
            mActionButton.setText(R.string.start_netbare)
            mRootCertButton.isEnabled = true
            mPrivateKeyButton.isEnabled = true
        }
        mUseRandomCertificatesRadio.isEnabled = true
        mProvideCertificatesRadio.isEnabled = true
    }

    private fun prepareNetBare() {
        // 安装自签证书
        if (!mJKS.isInstalled(this, JSK_ALIAS)) {
            try {
                mJKS.install(this, JSK_ALIAS, JSK_ALIAS)
            } catch (e: IOException) {
                // 安装失败
                Log.d("TEST", e.toString());
            }
            return
        }
        // 配置VPN
        val intent = NetBare.get().prepare()
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_PREPARE)
            return
        }
        // 启动NetBare服务
        mNetBare.start(NetBareConfig.defaultHttpConfig(mJKS,
                interceptorFactories()))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_PREPARE -> prepareNetBare()
                REQUEST_CODE_PICK_ROOT_CERTIFICATE -> {
                    if (data != null) {
                        val rootFileUri : Uri = data.data ?: Uri.parse("")
                        mRootFilePath = rootFileUri.path ?: ""
                        rootCertificatePath = FileUtils.getPath(this, rootFileUri) ?: ""
                        // Create keystore if both root certificate and private key have been provided
                        if (!mRootFilePath.isEmpty() && !mPrivateKeyFilePath.isEmpty()) {
                            createJKS()
                            mActionButton.isEnabled = true
                        }
                    }
                }
                REQUEST_CODE_PICK_PRIVATE_KEY -> {
                    if (data != null) {
                        val privateKeyFileUri : Uri = data.data ?: Uri.parse("")
                        mPrivateKeyFilePath = privateKeyFileUri.path ?: ""
                        privateKeyPath = FileUtils.getPath(this, privateKeyFileUri) ?: ""
                        // Create keystore if both root certificate and private key have been provided
                        if (!mRootFilePath.isEmpty() && !mPrivateKeyFilePath.isEmpty()) {
                            createJKS()
                            mActionButton.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun interceptorFactories() : List<HttpInterceptorFactory> {
        // 拦截器范例1：打印请求url
        val interceptor1 = HttpUrlPrintInterceptor.createFactory()
        // 注入器范例1：替换百度首页logo
        val injector1 = HttpInjectInterceptor.createFactory(BaiduLogoInjector())
        // 注入器范例2：修改发朋友圈定位
        val injector2 = HttpInjectInterceptor.createFactory(WechatLocationInjector())
        // 可以添加其它的拦截器，注入器
        // ...
        return listOf(interceptor1, injector1, injector2)
    }

    private fun isReadExternalPermissionGranted(): Boolean {
        val permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        return permission == PackageManager.PERMISSION_GRANTED
    }

    private fun makePermissionRequest() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_READ_EXTERNAL_STORAGE_PERMISSION_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Please grant read external storage permission to provide valid certificates",
                            Toast.LENGTH_LONG).show()
                    mUseRandomCertificatesRadio.isChecked = true
                    mProvideCertificatesRadio.isChecked = false
                } else {
                    mRootCertButton.isEnabled = true
                    mPrivateKeyButton.isEnabled = true
                    mActionButton.isEnabled = false
                }
            }
        }
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
}
