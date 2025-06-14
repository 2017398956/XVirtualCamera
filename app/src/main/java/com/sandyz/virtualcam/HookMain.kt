package com.sandyz.virtualcam

import android.annotation.SuppressLint
import android.app.Application
import android.app.Instrumentation
import android.content.res.XModuleResources
import android.content.res.XResources
import com.sandyz.virtualcam.hooks.IHook
import com.sandyz.virtualcam.hooks.VirtualCameraBiliSmile
import com.sandyz.virtualcam.hooks.VirtualCameraDy
import com.sandyz.virtualcam.hooks.VirtualCameraPdd
import com.sandyz.virtualcam.hooks.VirtualCameraWs
import com.sandyz.virtualcam.utils.HookUtils
import com.sandyz.virtualcam.utils.xLog
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.IXposedHookZygoteInit.StartupParam
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage


/**
 *@author sandyz987
 *@date 2023/11/17
 *@description
 */

class HookMain : IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {

    companion object {
        const val TAG = "HookMain"
        const val SELF_PACKAGE_NAME = "com.sandyz.virtualcam"
        var modulePath: String? = null
        private var moduleRes: String? = null
        private var xResources: XResources? = null

        @SuppressLint("UnsafeDynamicallyLoadedCode")
        private fun loadNative() {
            val libs = arrayOf(
//                "$modulePath/lib/arm64-v8a/libijkffmpeg.so",
//                "$modulePath/lib/arm64-v8a/libijksdl.so",
//                "$modulePath/lib/arm64-v8a/libijkplayer.so",
//                "$modulePath/lib/arm64-v8a/libencoder.so",
//
//                "$modulePath/lib/armeabi-v7a/libijkffmpeg.so",
//                "$modulePath/lib/armeabi-v7a/libijksdl.so",
//                "$modulePath/lib/armeabi-v7a/libijkplayer.so",
//                "$modulePath/lib/armeabi-v7a/libencoder.so",

                "$modulePath/lib/arm64/libijkffmpeg.so",
                "$modulePath/lib/arm64/libijksdl.so",
                "$modulePath/lib/arm64/libijkplayer.so",
                "$modulePath/lib/arm64/libencoder.so",

//                "$modulePath/lib/x86/libijkffmpeg.so",
//                "$modulePath/lib/x86/libijksdl.so",
//                "$modulePath/lib/x86/libijkplayer.so",
//                "$modulePath/lib/x86/libencoder.so",
//
//                "$modulePath/lib/x86_64/libijkffmpeg.so",
//                "$modulePath/lib/x86_64/libijksdl.so",
//                "$modulePath/lib/x86_64/libijkplayer.so",
//                "$modulePath/lib/x86_64/libencoder.so",

                )
            libs.forEach {
                try {
                    System.load(it)
                    xLog("loadNative success $it")
                } catch (e: Exception) {
                    xLog("loadNative failed:${e.localizedMessage}")
                }
            }
        }
    }

    private val hooks = listOf(
        VirtualCameraBiliSmile(),
        VirtualCameraDy(),
        VirtualCameraPdd(),
        VirtualCameraWs(),
    )

    override fun initZygote(startupParam: StartupParam) {
        modulePath = startupParam.modulePath.substring(0, startupParam.modulePath.lastIndexOf('/'))
        moduleRes = startupParam.modulePath
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam?) {
        xResources = resparam?.res
        var supported = (resparam?.packageName == SELF_PACKAGE_NAME)
        hooks.forEach {
            it.getSupportedPackages().forEach { pkg ->
                if (pkg == resparam?.packageName) {
                    supported = true
                }
            }
        }
        if (supported) {
            val modRes = XModuleResources.createInstance(moduleRes, resparam?.res)
            hooks.forEach {
                it.registerRes(modRes)
            }
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        HookUtils.init(lpparam)

        hooks.forEach {
//            var supported = (lpparam.packageName == SELF_PACKAGE_NAME)
            var supported = false
            it.getSupportedPackages().forEach { pkg ->
                if (pkg == lpparam.packageName) {
                    supported = true
                    return@forEach
                }
            }
            if (!supported) {
                xLog("init>>>>${it.getName()}>>>> unsupported! ===================== package: ${lpparam.packageName} process: ${lpparam.processName}")
            } else {
                xLog("init>>>>${it.getName()}>>>> package: ${lpparam.packageName} process: ${lpparam.processName}")
                loadNative()
                XposedHelpers.findAndHookMethod(
                    Instrumentation::class.java,
                    "callApplicationOnCreate",
                    Application::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            init(it, lpparam)
                        }
                    })
            }
            return@forEach
        }
    }

    private fun init(hook: IHook, lpparam: XC_LoadPackage.LoadPackageParam?) {
        hook.init(lpparam?.classLoader)
        hook.hook(lpparam)
    }

}