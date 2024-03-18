package com.fynd.extension.middleware;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

public class RetryInterceptor implements Interceptor {

    private int maxRetries = Integer.MAX_VALUE;

    public static long calculateRetryTime(int attempt) {
        long baseTime = 30 * 1000; // 30 seconds in milliseconds
        long increasePerAttempt = 60 * 1000; // 1 minute in milliseconds

        if (attempt <= 3) {
            return baseTime;
        } else {
            return (attempt - 3) * increasePerAttempt;
        }
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        int tryCount = 0;
        while (tryCount < maxRetries) {
            try {
                Response response = chain.proceed(chain.request());
                int responseCode = response.code();
                if (responseCode == 502 || responseCode == 503 || responseCode == 504) {
                    throw new IOException("Request failed - " + tryCount);
                } else {
                    return response;
                }
            } catch (IOException e) {
                if (tryCount == maxRetries - 1) {
                    throw e; // Stop retrying after reaching max retries
                }
                long time = calculateRetryTime(tryCount + 1);
                System.out.println("RetryInterceptor " + "Retrying request after " + time + "ms. Attempt: " + (tryCount + 1));
                try {
                    Thread.sleep(time);
                } catch (InterruptedException interruptedEx) {
                    // Handle the interruption here (optional)
                    System.out.println("RetryInterceptor Thread interrupted during retry " + interruptedEx);
                    throw new IOException("Thread interrupted during retry", interruptedEx); // Re-throw as IOException
                }

            }
            tryCount++;
        }

        return null; // This should never be reached
    }
}
