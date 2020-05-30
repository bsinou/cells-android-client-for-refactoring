package com.pydio.android.client.gui.dialogs.models;

import android.content.Context;

import com.pydio.android.client.R;
import com.pydio.android.client.data.nodes.NodeUtils;

import java.security.cert.X509Certificate;

public class DialogData {

    public Runnable action;
    public int iconRes;
    public String title;
    public String message;
    public String positiveText;

    public static DialogData removeFile(Context context, String name, boolean folder, boolean movedInTrash, Runnable r){
        DialogData data = new DialogData();
        data.iconRes = R.drawable.ic_trash_can_outline_grey600_48dp;
        if(movedInTrash){
            data.message = String.format(context.getString(R.string.will_be_moved_in_trash), name);
        } else {
            data.message = String.format(context.getString(R.string.will_be_permanently_deleted), name);
        }
        if(folder){
            data.title = context.getString(R.string.remove_folder);
        } else {
            data.title = context.getString(R.string.remove_file);
        }
        data.positiveText = context.getString(R.string.remove);
        data.action = r;
        return data;
    }

    public static DialogData confirm(Context context, String name, Runnable r){
        DialogData data = new DialogData();
        data.iconRes = R.drawable.ic_trash_can_outline_grey600_48dp;

        data.message = String.format(context.getString(R.string.account_will_be_removed), name);
        data.title = context.getString(R.string.remove_account);

        data.positiveText = context.getString(R.string.delete);
        data.action = r;
        return data;
    }

    public static DialogData confirmDownloadOnCellularData(Context context, String filename, long size, Runnable r){
        DialogData data = new DialogData();
        data.iconRes = R.drawable.transfer;
        String format = context.getString(R.string.download_on_cellular);
        data.message = String.format(format, filename, NodeUtils.stringSize(size));
        data.title = context.getString(R.string.download);
        data.positiveText = context.getString(R.string.download);
        data.action = r;
        return data;
    }

    public static DialogData confirmUploadOnCellularData(Context context, Runnable r) {
        DialogData data = new DialogData();
        data.iconRes = R.drawable.transfer;
        data.message = context.getString(R.string.upload_on_cellular_data);
        data.title = context.getString(R.string.upload);
        data.positiveText = context.getString(R.string.upload);
        data.action = r;
        return data;
    }

    public static DialogData confirmDownloadOnCellularData(Context context, String filename, String strSize, Runnable r){
        String size;
        try {
            size = NodeUtils.stringSize(Double.parseDouble(strSize));
        } catch (Exception e){
            size = context.getString(R.string.unknwon_size);
        }
        DialogData data = new DialogData();
        data.iconRes = R.drawable.transfer;
        data.message = String.format(context.getString(R.string.download_on_cellular), filename, size);
        data.title = context.getString(R.string.download);
        data.positiveText = context.getString(R.string.download);
        data.action = r;
        return data;
    }

    public static DialogData acceptCertificate(Context context, X509Certificate certificate, Runnable r){
        DialogData data = new DialogData();
        data.title = context.getString(R.string.server_certificate);
        data.message = certificate.toString().replace("\n", "\n\n");
        data.iconRes = R.drawable.security;
        data.positiveText = context.getString(R.string.accept_certificate);
        data.action = r;
        return data;
    }

}
