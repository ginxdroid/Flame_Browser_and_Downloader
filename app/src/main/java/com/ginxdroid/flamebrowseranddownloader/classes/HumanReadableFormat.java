package com.ginxdroid.flamebrowseranddownloader.classes;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class HumanReadableFormat {

    public static String calculateHumanReadableSize(double bytes, DecimalFormat dec)
    {
        try {
            double k = bytes / 1024;
            double m = (k / 1024);
            double g = (m / 1024);
            double t = (g / 1024);

            if(t > 1)
            {
                return dec.format(t).concat(" TB");
            } else if (g > 1)
            {
                return dec.format(g).concat(" GB");
            } else if(m > 1)
            {
                return dec.format(m).concat(" MB");
            } else if(k > 1)
            {
                return dec.format(k).concat(" KB");
            } else {
                return dec.format(bytes).concat(" B");
            }
        } catch (Exception e)
        {
            return "";
        }
    }

    public static String calculateHumanReadableSizeDm(double bytes, DecimalFormat speedDec)
    {
        try {
            double k = bytes / 1024;
            double m = (k / 1024);
            double g = (m / 1024);
            double t = (g / 1024);

            if(t > 1)
            {
                return speedDec.format(t).concat(" TB/S");
            } else if (g > 1)
            {
                return speedDec.format(g).concat(" GB/S");
            } else if(m > 1)
            {
                return speedDec.format(m).concat(" MB/S");
            } else if(k > 1)
            {
                return speedDec.format(k).concat(" KB/S");
            } else {
                return speedDec.format(bytes).concat(" B/S");
            }
        } catch (Exception e)
        {
            return "";
        }
    }

    public static String calculateHumanReadableTimeDM(double bytes, long seconds, DecimalFormat dec)
    {
        try {
            String size;

            double k = bytes / 1024;
            double m = (k / 1024);
            double g = (m / 1024);
            double t = (g / 1024);

            if(t > 1)
            {
                size = dec.format(t).concat(" TB) ");
            } else if (g > 1) {
                size = dec.format(g).concat(" GB) ");
            } else if (m > 1) {
                size = dec.format(m).concat(" MB) ");
            } else if (k > 1) {
                size = dec.format(k).concat(" KB) ");
            } else {
                size = dec.format(bytes).concat(" B) ");
            }

            long longMinutes = TimeUnit.SECONDS.toMinutes(seconds);
            long longHours = TimeUnit.SECONDS.toHours(seconds);
            long days = TimeUnit.SECONDS.toDays(seconds);

            long hours = longHours - (days * 24);
            long minutes = longMinutes - (longHours * 60);
            long second = TimeUnit.SECONDS.toSeconds(seconds) - (longMinutes * 60);

            if(days > 1)
            {
                return "~("+size+days+" d "+hours+" h left";
            } else if (hours >= 1) {
                return "~("+size+hours+" h "+minutes+" min left";
            } else if (minutes >= 1) {
                return "~("+size+minutes+" min "+second+" s left";
            }else {
                return "~("+size+second+" s left";
            }

        } catch (Exception e)
        {
            return "";
        }
    }

    public static String calculateHumanReadableSizeChunkDM(double bytes, DecimalFormat dec)
    {
        try {
            double k = bytes / 1024;
            double m = (k / 1024);
            double g = (m / 1024);
            double t = (g / 1024);

            if(t > 1)
            {
                return dec.format(t).concat(" TB downloaded");
            } else if (g > 1) {
                return dec.format(g).concat(" GB downloaded");
            } else if (m > 1) {
                return dec.format(m).concat(" MB downloaded");
            } else if (k > 1) {
                return dec.format(k).concat(" KB downloaded");
            } else {
                return dec.format(bytes).concat(" B downloaded");
            }
        } catch (Exception e)
        {
            return "";
        }
    }
}

//Exception Status
//0 = "Downloading"
//1 = "IOException"
//2 = "DirectoryNotFoundException"
//3 = "MalformedURLException"
//4 = "ServerTemporarilyUnavailable"
//5 = "ConnectionTimedOutException"
//6 = "DirectoryNotFoundException"
//7 = "SeveralRetriedException"
//8 = "Unknown"
// = "NetworkInterruptedAndPRNOException"
//"OutOfSpaceException"

//Download Task Status
// CURRENT_STATUS
//         case "Deleting":
//         values.put(CURRENT_STATUS, 0);
//         case "Started":
//         values.put(CURRENT_STATUS, 1);
//         case "Downloading":
//         values.put(CURRENT_STATUS, 2);
//         case "Resume":
//         values.put(CURRENT_STATUS, 3);
//         case "Pause":
//         values.put(CURRENT_STATUS, 4);
//         case "Error":
//         values.put(CURRENT_STATUS, 5);
//         case "Waiting":
//         values.put(CURRENT_STATUS, 6);
//         case "downloadComplete":
//         values.put(CURRENT_STATUS, 7);