//! `com.aistudio.aerofast.trmst.data.NativeEngine.call(method, argsJson)` for the Android app.
//! A panic inside the engine is caught and returned as an error, so the app shows a message instead of closing.

use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_com_aistudio_aerofast_trmst_data_NativeEngine_call<'l>(mut env: JNIEnv<'l>, _class: JClass<'l>, method: JString<'l>, args: JString<'l>) -> jstring {
    let method: String = env.get_string(&method).map(|s| s.into()).unwrap_or_default();
    let args: String = env.get_string(&args).map(|s| s.into()).unwrap_or_default();
    let out = std::panic::catch_unwind(|| crate::bridge::dispatch(&method, &args))
        .unwrap_or_else(|_| serde_json::json!({ "err": format!("{method}: internal engine error") }).to_string());
    match env.new_string(out) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
