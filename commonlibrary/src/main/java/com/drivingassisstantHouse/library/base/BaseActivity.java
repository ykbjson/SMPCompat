package com.drivingassisstantHouse.library.base;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.drivingassisstantHouse.library.R;
import com.drivingassisstantHouse.library.data.OnNetWorkEvent;
import com.drivingassisstantHouse.library.tools.PageCache;
import com.drivingassisstantHouse.library.tools.SLog;
import com.mcxiaoke.bus.annotation.BusReceiver;
import com.umeng.analytics.MobclickAgent;
import com.umeng.message.PushAgent;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * android 系统中的四大组件之一Activity基类
 *
 * @author sunji
 * @version 1.0
 */
public abstract class BaseActivity extends AppCompatActivity implements IBaseActivity {
    private final String TAG=getClass().getName();
    /**
     * 当前Activity渲染的视图View
     **/
    protected View mContextView = null;
    /**
     * 动画类型
     **/
    private int mAnimationType = NONE;
    /**
     * 是否运行截屏
     **/
    private boolean isCanScreenshot = true;
    /**
     * 共通操作
     **/
    protected Operation mOperation = null;

    protected BaseHandler baseHandler = new BaseHandler(this);

    protected Toolbar toolbar;

    protected Unbinder unbinder;

    protected static class BaseHandler extends Handler {
        private WeakReference<IBaseActivity> baseActivity;

        public BaseHandler(IBaseActivity baseActivity) {
            this.baseActivity = new WeakReference<>(baseActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            baseActivity.get().handleMessage(msg);
            super.handleMessage(msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SLog.d(TAG+"-->onCreate()");
        // 将当前Activity压入栈
        PageCache.put(this);

        // 实例化共通操作
        mOperation = new Operation(this);

        // 初始化参数
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mAnimationType = bundle.getInt(ANIMATION_TYPE, NONE);
        } else {
            bundle = new Bundle();
        }
        mContextView = getLayoutInflater().inflate(bindLayout(), null, false);
        // 设置渲染视图View
        setContentView(mContextView);
        unbinder=ButterKnife.bind(this);//使用bufferKnife
        initParms(bundle);
        // 初始化控件

        initView(mContextView);

        // 业务操作
        doBusiness(this);

        // 显示VoerFlowMenu
        displayOverflowMenu(this);

        // 是否可以截屏
        if (!isCanScreenshot) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        PushAgent.getInstance(this).onAppStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        SLog.d(TAG+"-->onRestart()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        SLog.d(TAG+"-->onStart()");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (null != mContextView) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isActive()) {
                imm.hideSoftInputFromWindow(mContextView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        MobclickAgent.onPageStart(getClass().getName());
        SLog.d(TAG+"-->onResume()");
        resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        MobclickAgent.onPageEnd(getClass().getName());
        SLog.d(TAG+"-->onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        SLog.d(TAG+"-->onStop()");
    }

    @Override
    protected void onDestroy() {
        destroy();
        if (null!=unbinder){
            unbinder.unbind();
        }
        super.onDestroy();
        PageCache.remove(this);
        SLog.d(TAG+"-->onDestroy()");
    }

    /**
     * 显示Actionbar菜单图标
     */
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);// 显示
                } catch (Exception e) {
                    SLog.e("onMenuOpened-->" + e.getMessage());
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    /**
     * 显示OverFlowMenu按钮
     *
     * @param mContext 上下文Context
     */
    public void displayOverflowMenu(Context mContext) {
        try {
            ViewConfiguration config = ViewConfiguration.get(mContext);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);// 显示
            }
        } catch (Exception e) {
            SLog.e(e.getMessage());
        }
    }

    /**
     * 设置是否可截屏
     *
     * @param isCanScreenshot
     */
    public void setCanScreenshot(boolean isCanScreenshot) {
        this.isCanScreenshot = isCanScreenshot;
    }

    /**
     * Actionbar点击返回键关闭事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        mOperation.finishActivity(this, Operation.ENTER_TYPE_LEFT, RESULT_CANCELED, null);
    }

    public void finish() {
        super.finish();
        switch (mAnimationType) {
            case IBaseActivity.LEFT_RIGHT:
                overridePendingTransition(R.anim.enter_lefttoright, R.anim.exit_lefttoright);
                break;
            case IBaseActivity.TOP_BOTTOM:
                overridePendingTransition(0, BaseView.gainResId(this, BaseView.ANIM, "base_push_up_out"));
                break;
            case IBaseActivity.FADE_IN_OUT:
                overridePendingTransition(0, BaseView.gainResId(this, BaseView.ANIM, "base_fade_out"));
                break;
            default:
                break;
        }
        mAnimationType = NONE;
    }

    @BusReceiver
    public void onMainNetWorkEvent(OnNetWorkEvent event) {
        onNetWorkChanged(event.connected);
    }

    @Override
    public void onNetWorkChanged(boolean connected) {

    }

    @Override
    public void handleMessage(Message msg) {

    }
}
