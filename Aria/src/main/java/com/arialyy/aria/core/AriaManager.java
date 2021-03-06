/*
 * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arialyy.aria.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import com.arialyy.aria.core.command.CmdFactory;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.core.command.IDownloadCmd;
import com.arialyy.aria.util.Configuration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by lyy on 2016/12/1.
 * https://github.com/AriaLyy/Aria
 * Aria管理器，任务操作在这里执行
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH) public class AriaManager {
  private static final    String                  TAG      = "AriaManager";
  private static final    Object                  LOCK     = new Object();
  private static volatile AriaManager             INSTANCE = null;
  private                 Map<String, AMReceiver> mTargets = new HashMap<>();
  private DownloadManager mManager;
  private LifeCallback    mLifeCallback;

  private AriaManager(Context context) {
    regAppLifeCallback(context);
    mManager = DownloadManager.init(context);
  }

  static AriaManager getInstance(Context context) {
    if (INSTANCE == null) {
      synchronized (LOCK) {
        INSTANCE = new AriaManager(context);
      }
    }
    return INSTANCE;
  }

  AMReceiver get(Object obj) {
    return getTarget(obj);
  }

  /**
   * 获取下载列表
   */
  public List<DownloadEntity> getDownloadList(){
    return DownloadEntity.findAllData(DownloadEntity.class);
  }

  /**
   * 通过下载链接获取下载实体
   */
  public DownloadEntity getDownloadEntity(String downloadUrl) {
    if (TextUtils.isEmpty(downloadUrl)) {
      throw new IllegalArgumentException("下载链接不能为null");
    }
    return DownloadEntity.findData(DownloadEntity.class, new String[] { "downloadUrl" },
        new String[] { downloadUrl });
  }

  /**
   * 停止所有正在执行的任务
   */
  public void stopAllTask() {
    List<DownloadEntity> allEntity = mManager.getAllDownloadEntity();
    List<IDownloadCmd>   stopCmds  = new ArrayList<>();
    for (DownloadEntity entity : allEntity) {
      if (entity.getState() == DownloadEntity.STATE_DOWNLOAD_ING) {
        stopCmds.add(CommonUtil.createCmd(entity, CmdFactory.TASK_STOP));
      }
    }
    mManager.setCmds(stopCmds).exe();
  }

  /**
   * 设置下载超时时间
   */
  @Deprecated
  private AriaManager setTimeOut(int timeOut) {
    Configuration.getInstance().setTimeOut(timeOut);
    return this;
  }

  /**
   * 设置下载失败重试次数
   */
  public AriaManager setReTryNum(int reTryNum) {
    Configuration.getInstance().setReTryNum(reTryNum);
    return this;
  }

  /**
   * 设置下载失败重试间隔
   */
  public AriaManager setReTryInterval(int interval) {
    Configuration.getInstance().setReTryInterval(interval);
    return this;
  }

  /**
   * 是否打开下载广播
   */
  public AriaManager openBroadcast(boolean open) {
    Configuration.getInstance().setOpenBroadcast(open);
    return this;
  }

  /**
   * 设置最大下载数，最大下载数不能小于1
   *
   * @param maxDownloadNum 最大下载数
   */
  public AriaManager setMaxDownloadNum(int maxDownloadNum) {
    if (maxDownloadNum < 1) {
      Log.w(TAG, "最大任务数不能小于 1");
      return this;
    }
    mManager.getTaskQueue().setDownloadNum(maxDownloadNum);
    return this;
  }

  /**
   * 删除所有任务
   */
  public void cancelAllTask() {
    List<DownloadEntity> allEntity  = mManager.getAllDownloadEntity();
    List<IDownloadCmd>   cancelCmds = new ArrayList<>();
    for (DownloadEntity entity : allEntity) {
      cancelCmds.add(CommonUtil.createCmd(entity, CmdFactory.TASK_CANCEL));
    }
    mManager.setCmds(cancelCmds).exe();
    Set<String> keys = mTargets.keySet();
    for (String key : keys) {
      AMReceiver target = mTargets.get(key);
      target.removeSchedulerListener();
      mTargets.remove(key);
    }
  }

  private AMReceiver putTarget(Object obj) {
    String     clsName = obj.getClass().getName();
    AMReceiver target  = mTargets.get(clsName);
    if (target == null) {
      target = new AMReceiver();
      target.obj = obj;
      if (obj instanceof android.support.v4.app.Fragment) {
        clsName += "_" + ((Fragment) obj).getActivity().getClass().getName();
      } else if (obj instanceof android.app.Fragment) {
        clsName += "_" + ((android.app.Fragment) obj).getActivity().getClass().getName();
      }
      mTargets.put(clsName, target);
    }
    return target;
  }

  private AMReceiver getTarget(Object obj) {
    AMReceiver target = mTargets.get(obj.getClass().getName());
    if (target == null) {
      target = putTarget(obj);
    }
    return target;
  }

  /**
   * 注册APP生命周期回调
   */
  private void regAppLifeCallback(Context context) {
    Context app = context.getApplicationContext();
    if (app instanceof Application) {
      mLifeCallback = new LifeCallback();
      ((Application) app).registerActivityLifecycleCallbacks(mLifeCallback);
    }
  }

  /**
   * Activity生命周期
   */
  private class LifeCallback implements Application.ActivityLifecycleCallbacks {

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override public void onActivityStarted(Activity activity) {

    }

    @Override public void onActivityResumed(Activity activity) {

    }

    @Override public void onActivityPaused(Activity activity) {

    }

    @Override public void onActivityStopped(Activity activity) {

    }

    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override public void onActivityDestroyed(Activity activity) {
      Set<String> keys = mTargets.keySet();
      for (String key : keys) {
        String clsName = activity.getClass().getName();
        if (key.equals(clsName) || key.contains(clsName)) {
          AMReceiver target = mTargets.get(key);
          if (target.obj != null) {
            if (target.obj instanceof Application || target.obj instanceof Service) break;
            target.removeSchedulerListener();
            mTargets.remove(key);
          }
          break;
        }
      }
    }
  }
}
