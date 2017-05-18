package com.example.lkduy.remotehelper;

import android.graphics.Path;

/**
 * Created by lkduy on 4/8/2017.
 */
public class PathWithTime {
    int _id;
    Path _path;
    long _timeStamp;
    public PathWithTime(){
        _path = new Path();
    }
    public int get_id(){return _id; }
    public void set_id(int id){_id = id;}
    public Path getPath(){
        return _path;
    }
    public void set_timeStamp(long t){
        _timeStamp = t;
    }
    public long get_timeStamp(){
        return _timeStamp;
    }
}
