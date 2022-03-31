package com.networknt.limit;

import java.util.concurrent.TimeUnit;

public class LimitQuota {
    int value;
    TimeUnit unit;

    public  LimitQuota() {

    }

    public  LimitQuota(String item) {
      if (item!=null && item.indexOf("/")!=-1) {
          this.value = Integer.parseInt(item.substring(0, item.indexOf("/")).trim());
          String u = item.substring(item.indexOf("/")+1).trim();
          if ("d".equalsIgnoreCase(u)) {
              unit = TimeUnit.DAYS;
          } else if ("h".equalsIgnoreCase(u)) {
              unit = TimeUnit.HOURS;
          } else if ("m".equalsIgnoreCase(u)) {
              unit = TimeUnit.MINUTES;
          } else {
              unit = TimeUnit.SECONDS;
          }
      }
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public void setUnit(TimeUnit unit) {
        this.unit = unit;
    }
}
