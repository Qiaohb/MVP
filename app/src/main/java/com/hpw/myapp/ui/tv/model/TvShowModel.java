package com.hpw.myapp.ui.tv.model;

import com.hpw.mvpframe.data.net.RxService;
import com.hpw.mvpframe.utils.helper.RxUtil;
import com.hpw.myapp.api.TvApi;
import com.hpw.myapp.ui.tv.contract.TvContract;

import rx.Observable;

/**
 * Created by hpw on 16/12/3.
 */

public class TvShowModel implements TvContract.TvShowModel {
    @Override
    public Observable<TvShowBean> onTvShow(String url) {
        return RxService.createApi(TvApi.class)
                .onTvShow(url)
                .compose(RxUtil.rxSchedulerHelper());
    }
}
