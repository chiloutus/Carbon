package com.garylynam.carbon;

/**
 * Created by garylaptop on 07/04/15.
 */
public class DelayHandler {
    DelayHandler(){}

    public static void postDelayed(int delay){
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
