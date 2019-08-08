package com.xinchen.netty.time;

import java.util.Date;

/**
 *
 * POJO对象处理时间
 * @author xinchen
 * @version 1.0
 * @date 08/08/2019 15:57
 */
public class UnixTIme {

    private final long value;

    public UnixTIme(){
        this(System.currentTimeMillis() / 1000L + 2208988800L);
    }

    public UnixTIme(long value) {
        this.value = value;
    }

    public long value(){
        return value;
    }

    @Override
    public String toString() {
        return new Date((value() - 2208988800L) * 1000L).toString();
    }
}
