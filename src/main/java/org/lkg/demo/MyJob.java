package org.lkg.demo;

import com.xxl.job.core.context.XxlJobHelper;
import org.lkg.core.ctl.AbstractTaskExecutor;
import org.lkg.core.ctl.TaskCtlGenerator;
import org.lkg.po.TaskPo;
import org.lkg.util.JacksonUtil;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

/**
 * @author likaiguang
 * @date 2023/4/25 10:28 下午
 */
public class MyJob extends AbstractTaskExecutor<MyJob.JobParam> {


    @Override
    protected TaskPo.InitialSnapShot generateGlobalTask(JobParam jobParam) {
        return TaskCtlGenerator.getTask("", "");
    }

    @Override
    public void doAnything(Collection<?> list) {
        // do your biz logic
    }

    @Override
    public ExecutorService executorService() {
        return null;
    }

    @Override
    public boolean isRun() {
        return true;
    }

    @Override
    public JobParam getParam() {
        return JacksonUtil.readValue(XxlJobHelper.getJobParam(), JobParam.class);
    }

    public static class JobParam {
        // what your job param
    }
}
