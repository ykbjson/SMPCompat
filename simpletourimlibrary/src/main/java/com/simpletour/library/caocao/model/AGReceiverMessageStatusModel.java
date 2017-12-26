package com.simpletour.library.caocao.model;

import com.simpletour.library.caocao.annoations.FieldId;
import com.simpletour.library.caocao.base.Marshal;

/**
 * 包名：com.simpletour.library.caocao.model
 * 描述：接收消息状态模型
 * 创建者：yankebin
 * 日期：2017/5/18
 */
public final class AGReceiverMessageStatusModel implements Marshal {
    @FieldId(1)
    public Integer readStatus;

    public AGReceiverMessageStatusModel() {
    }

    public void decode(int idx, Object value) {
        switch(idx) {
        case 1:
            this.readStatus = (Integer)value;
        default:
        }
    }
}