package com.bearever.async.chain.lifecycler.base;


import android.app.Activity;

/**
 * 生命周期状态的基类
 *
 * @author :  luoming    luomingbear@163.com
 * @date :  2019/7/24
 **/
public interface AsyncChainLifeCycleListener {

    /**
     * 当 {@link android.app.Fragment#onDestroy()}} 或者 {@link
     * Activity#onDestroy()}  的时候执行，表示依附的对象销毁了
     *
     * @param lifeCycle 被销毁的生命周期类
     */
    void onDestroy(AsyncChainLifeCycle lifeCycle);
}
