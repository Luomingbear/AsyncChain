# AsyncChain

基于AndroidX的异步链式调用库，方便的实现异步链式调用，例如`A->B->C-D`的依赖任务，同时可以解决线程切换写`Handler`的烦恼。

## 使用

```java
AsyncChain
    .withWork(new AsyncChainRunnable(){
        void run(AsyncChainTask task){
            //执行一个异步操作
            doSomething1(new someCallback1(){
                void callback(newResult){
                    //标识一个行为出错了
                    task.onError(error)
                    //标识一个异步操作的结束，进行下一步操作
                    task.onNext(newResult);
                }
            })
        }
})
    .withMain(new AsyncChainRunnable(){
        void run(AsyncChainTask task){
            //使用异步操作的结果更新UI
            updateUI(lastResult);
            //标识整个异步链式结束了，即使后面还有行为没有执行也不会继续下去了
            task.onComplete(lastResult);
        }
})
    .errorMain(new AsyncChainErrorCallback(){
        error(AsyncChainError error){
            //在主线程处理错误
            //一旦error*方法执行，异步链就中断了
        }
    })
    .go(activity/fragment/view/context);
```

## 执行下一步

因为 `with*()` 方法里面的行为可能是异步的，所以没法使用返回值直接判断一个行为的结束，这里需要我们主动告诉 `AsyncChain` 我执行完毕了，即在每一个 `AsyncChainRunnable` 的里面都需要调用一次 `task` 来告诉框架异步操作执行完毕了，例如：

```java
AsyncChain
    .withWork(new AsyncChainRunnable(){
        void run(AsyncChainTask task){
            //执行一个异步操作
            doSomething1(new someCallback1(){
                void callback(newResult){
                    //标识一个异步操作的结束
                    //标识一个异步操作的结束
                    //标识一个异步操作的结束
                    task.onNext(newResult);
                }
            })
        }
})
    .....
```

这里 `AsyncChainTask` 有四个方法，一个行为结束，想要执行下一个异步行为的话，需要调用 `onNext(newResult)` ，如果想要停止整个异步链则调用 `onComplete()` ，如果一个步骤出了错的话则执行 `onError(error)` ，这个操作会通过 `AsyncChainErrorCallback` 回调回来。

```java
//AsyncChainTask.java
 
/**
* 某一个操作成功结束了，执行下一步
* @param newResult 当前异步行为的结果，传递给下一个步骤使用，可以传递null
*/
public void onNext(T newResult);
 
/**
* 某一个操作失败了
* @param error 错误信息
*/
public void onError(AsyncChainError error);
 
/**
* 整个异步链结束了，即使还有未执行异步操作，也不会执行了
*/
public void onComplete();
 
/**
 * 获取上一个一步行为的结果
 *
 * @return 来自于上一个步骤的{@link #onNext(lastResult)}
 */
public T getLastResult();
```

## 错误处理

当一个异步操作执行的时候出现了错误，会被框架 `try/catch` ，想要在出现错误的时候执行某些行为，只需要 `.error*(@NonNull AsyncChainErrorCallback errorCallback)` 即可，其中 `error(callback)` 表示在原有的线程执行， `errorMain(callback)` 表示在主线程执行， `errorWork(callback)` 表示在工作线程执行。如果不想处理错误，则不需要调用 `error*(callback)` 。

```java
//AsyncChainLink.java
...
 
/**
 * 在报错的线程处理错误
 *
 * @param errorCallback 执行错误的时候的回调
 */
public AsyncChainLinkGo error(@NonNull AsyncChainErrorCallback errorCallback);
 
/**
 * 在工作线程处理错误
 *
 * @param errorCallback 执行错误的时候的回调
 */
public AsyncChainLinkGo errorWork(@NonNull AsyncChainErrorCallback errorCallback);
 
/**
 * 在主线程处理错误
 *
 * @param errorCallback 执行错误的时候的回调
 */
public AsyncChainLinkGo errorMain(@NonNull AsyncChainErrorCallback errorCallback);
```

## 延时操作

想要让一个行为延迟执行，这个也可以很方便的实现，只需要调用 `delay(毫秒)` 即可，例如：

```java
//延迟1000毫秒，然后Toast提示
AsyncChain.delay(1000)
        .withMain(new AsyncChainRunnable() {
            @Override
            public void run(AsyncChainTask task) throws Exception {
                Toast.makeText(MainActivity.this,"你好世界",Toast.LENGTH_SHORT).show();
                task.onComplete();
            }
        }).go(this);
```

## 简述内部实现

`AsyncChain.with*` 创建一个新的 `AsyncChainLink` 类， `AsyncChainLink` 实现了 `AsyncChainLifeCycleListner` 接口，当 `AsyncChainLink.go(*)` 时会通过 `AsyncChainManger` 获取一个 `AsyncChainLifeCycle` 生命周期的实现，然后将 `Link` 添加到 `AsyncChainLifeCycle` 的生命周期状态监听接口列表里，同时执行 `AsyncChainLink.start()` 。当一个异步操作执行完毕调用 `task.onNext(newResult)` 的时候，本质上调用的是 `AsyncChainManager.next(runId,reslut)` ，因为 `AsyncChainManager` 持有里所有生命周期实现的Map，而生命周期实现里面持有了需要执行的 `AsyncChainLink` 列表，就可以找到这个异步操作的下一个操作进行执行。
![img](https://file.2fun.xyz/async_chain_uml_20190725.png)
