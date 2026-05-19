package com.aliothmoon.maameow.root;

import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;

import com.aliothmoon.maameow.third.Ln;

public final class RootServiceStarter {

    private static final String TAG = "RootServiceStarter";
    private static final int DESTROY_TRANSACTION_CODE = 16777115;

    private RootServiceStarter() {
    }

    public static void main(String[] args) {
        System.err.println("[RootServiceStarter] main() entry");
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        RootUserService.CreatedService createdService = RootUserService.create(args);
        if (createdService == null) {
            System.err.println("[RootServiceStarter] RootUserService.create() returned null");
            System.exit(1);
            return;
        }
        System.err.println("[RootServiceStarter] RootUserService.create() ok, token=" + createdService.token());

        if (!sendBinder(createdService)) {
            System.err.println("[RootServiceStarter] sendBinder() failed");
            System.exit(1);
            return;
        }
        System.err.println("[RootServiceStarter] sendBinder() ok, entering Looper");

        Looper.loop();
        System.exit(0);
    }

    private static boolean sendBinder(RootUserService.CreatedService createdService) {
        IBinder lifecycleBinder = RootServiceBootstrapClient.attachRemoteService(
                createdService.packageName(),
                createdService.userId(),
                createdService.token(),
                createdService.service()
        );
        if (lifecycleBinder == null) {
            return false;
        }

        try {
            lifecycleBinder.linkToDeath(() -> {
                Ln.i(TAG + ": app process died, destroying root service");
                destroyService(createdService.service());
                Ln.i(TAG + ": root service destroy signal sent, exiting");
                System.exit(0);
            }, 0);
            return true;
        } catch (Throwable tr) {
            Ln.e(TAG + ": failed to link app lifecycle binder", tr);
            return false;
        }
    }

    private static void destroyService(IBinder service) {
        if (service == null || !service.pingBinder()) {
            return;
        }

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            String descriptor = service.getInterfaceDescriptor();
            if (descriptor != null) {
                data.writeInterfaceToken(descriptor);
            }
            service.transact(DESTROY_TRANSACTION_CODE, data, reply, Binder.FLAG_ONEWAY);
        } catch (Throwable tr) {
            Ln.w(TAG + ": destroy root remote service failed", tr);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
