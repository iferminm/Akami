package com.jiahaoliuliu.akami.utils;

import com.jiahaoliuliu.akami.model.ITransactions;

import java.util.Calendar;

/**
 *
 * Temporally solution. //TODO: Move it to the proper class
 * Created by jiahaoliuliu on 7/8/16.
 */
public class HeaderUtility {

    public static long getHeaderMonthlyKeyByTransaction(ITransactions transaction) {
        // Generate the key
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(transaction.getDate());
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }
}