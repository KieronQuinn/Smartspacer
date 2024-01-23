package android.content;

import android.net.Uri;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.FileNotFoundException;

public interface IContentProvider {

    String getType(Uri url) throws RemoteException;

    String[] getStreamTypes(Uri url, String mimeTypeFilter) throws RemoteException;

    @RequiresApi(31)
    ParcelFileDescriptor openFile(@NonNull AttributionSource attributionSource,
                                  Uri url, String mode, ICancellationSignal signal)
            throws RemoteException, FileNotFoundException;

    @RequiresApi(30)
    ParcelFileDescriptor openFile(String callingPkg, @Nullable String attributionTag,
                                  Uri url, String mode, ICancellationSignal signal, IBinder callerToken)
            throws RemoteException, FileNotFoundException;

    ParcelFileDescriptor openFile(
            String callingPkg, Uri url, String mode, ICancellationSignal signal,
            IBinder callerToken)
            throws RemoteException, FileNotFoundException;

}
