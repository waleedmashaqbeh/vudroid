package org.vudroid.djvudroid.codec;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import org.vudroid.core.VuDroidLibraryLoader;
import org.vudroid.core.codec.CodecContext;

import java.util.concurrent.Semaphore;

public class DjvuContext implements Runnable, CodecContext
{
    static
    {
        VuDroidLibraryLoader.load();        
    }

    private final long contextHandle;
    private static final String DJVU_DROID_CODEC_LIBRARY = "DjvuDroidCodecLibrary";
    private final Object waitObject = new Object();
    private final Semaphore docSemaphore = new Semaphore(0);

    public DjvuContext()
    {
        this.contextHandle = create();
        new Thread(this).start();
    }

    public DjvuDocument openDocument(Uri uri)
    {
        final DjvuDocument djvuDocument = DjvuDocument.openDocument(uri.getPath(), this, waitObject);
        try
        {
            docSemaphore.acquire();
        } catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        return djvuDocument;
    }

    long getContextHandle()
    {
        return contextHandle;
    }

    public void run()
    {
        for(;;)
        {
            try
            {
                handleMessage(contextHandle);
                synchronized (waitObject)
                {
                    waitObject.notifyAll();
                }
            }
            catch (Exception e)
            {
                Log.e(DJVU_DROID_CODEC_LIBRARY, "Codec error", e);
            }
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void handleDocInfo()
    {
        docSemaphore.release();
    }

    public void setContentResolver(ContentResolver contentResolver)
    {
    }

    @Override
    protected void finalize() throws Throwable
    {
        free(contextHandle);
        super.finalize();
    }

    private static native long create();
    private static native void free(long contextHandle);
    private native void handleMessage(long contextHandle);
}