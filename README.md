# Automatic Android SDK

1. [Creating an App](#creating_an_app)
2. [Adding the SDK to your Android App](#adding_the_sdk)
3. [Initialization + Authentication](#init_and_auth)
4. [ELM Streaming](#streaming_sdk)
5. [Supported ELM327 Commands](#supported_commands)
6. [Receiving Real-Time Events](#events)
7. [Testing With An ECU Simulator](#simulator)
8. [Making REST API Calls](#rest_api)

---

**Note: Please file any bugs in the [Github Issues](https://github.com/Automatic/automatic-android-sdk/issues) tracker**

---

The Automatic SDK is the best way to build Android apps powered by [Automatic](http://automatic.com).

With the Automatic Android SDK, your users can log in to your app with their [Automatic](http://automatic.com) accounts. Think _Facebook_ or _Twitter_ login—but rather than bringing a users' social graph, instead unlocking a wealth of automotive data that you can use to supercharge your app.

<img src='https://github.com/automatic/automatic-android-sdk/blob/master/README/login_button_example.png?raw=true' alt='Log in with Automatic' height='102' width='337'/>

Once a user approves your app's request to access their data, your app could:

- Access your users' trips to analyze driving habits
- Query your users' cars to provide up-to-date resale values estimates
- Populate your users' profiles without a lengthy signup form
- Receive "pushed" events such as Ignition On, Ignition Off, etc.
- :sparkles: _so much more_ :sparkles:

We can't wait to see what you build. Let's get to it!

## 1. <a name="creating_an_app"></a>Creating an App

- Each SDK app must first create an app on the [Automatic Developer site][developers].
- **Important:** Currently, apps must have the Client Type field set to "Public / Native" or authentication will fail.

## 2. <a name="adding_the_sdk"></a>Adding the SDK to your Android App

1. Add the SDK aar library to your project. Add the following line to your `build.gradle`, within your `dependencies {}` block:

	```gradle
	compile(name:'automatic-android-sdk-release', ext:'aar')
	```

2. Add your client id (found within the Automatic [Developer Apps Manager](https://developer.automatic.com/dashboard))  to your AndroidManifest.xml, inside your `<application>` tag:

	```xml
	<meta-data android:name="com.automatic.sdk.client_id" android:value="your_client_id" />
	```

## 3. <a name="init_and_auth"></a>Initialization + Authentication

1. Before your app can access any of the SDKs features, your user must authenticate with Automatic.  To allow them to do this from inside your app, you must include a "Login With Automatic" button to your app's UI:

	```xml
	<com.automatic.android.sdk.LoginButton
    	android:id="@+id/automatic_login"
    	android:layout_width="wrap_content"
    	android:layout_height="wrap_content" />
	```

2. Initialize the SDK, ideally within an Application context or any other context such as your Launcher which is run every time your app loads.  Pass it the scopes which you have access to.  Note that if your scopes are invalid, you will receive an "Invalid Request" upon authorizing.  To receive "pushed" events such as Ignition On / Off, Location, be sure to call `useServiceBinding()`:

	```java
	// Scopes that your application has registered with Automatic on the Automatic developer apps manager page.
	public static final Scope[] scopes = {Scope.Public, Scope.VehicleVin, Scope.Trips, Scope.Location, Scope.VehicleEvents, Scope.VehicleProfile, Scope.UserProfile, Scope.Behavior, Scope.AdapterBasic};
	```

	```java
	Automatic.initialize(
        new Automatic.Builder(this)
            .addScopes(scopes)
            .useServiceBinding()
            ... etc);
	```

3. Auto login. If user has an Automatic account set up on their device, your app could login with that same Automatic account:

	```java
    Automatic.initialize(
        new Automatic.Builder(this)
            .addScopes(scopes)
            .useServiceBinding()
            .autoLogIn(true)
            ... etc);
    ```

	By default `autoLogIn` is set to `true`.

	Set `autoLogIn` to `false` for web login, where user is presented a web view to enter their username and password:

	```java
    Automatic.initialize(
        new Automatic.Builder(this)
            ...
            .autoLogIn(false)
            ... etc);
    ```

4. In the Activity you're using to handle the "Login With Automatic" flow, pass the SDK a context, and an implementation of `AutomaticLoginCallbacks`:
	- Login using the "Login With Automatic" button. Make a call to `addLoginButton()` and pass in a reference to your LoginButton:

        ```java
        Automatic.get().addLoginButton(mLoginButton, mYourContext, AutomaticLoginCallbacks);
        ```

	- You can also login without the login button:

        ```java
        // pass in a callback to handle success / failure
        Automatic.get().setLoginCallbackListener(new AutomaticLoginCallbacks() {
                @Override
                public void onLoginSuccess() {
                    // success!
                }
                @Override
                public void onLoginFailure(SdkError error) {
                    // failed.
                }
            });
        Automatic.get().loginWithAutomatic(mYourContext);
        ```

        Or simply:

        ```java
        Automatic.get().loginWithAutomatic(mYourContext, AutomaticLoginCallbacks);
        ```

## 4. <a name="streaming_sdk"></a>ELM Streaming

**Note:** Before using the Streaming SDK, you must have already created an Automatic account using the [core app](https://play.google.com/store/apps/details?id=com.automatic), and completed the Setup process with each adapter + phone pair that you plan to use.  Each device's firmware must also be updated to use version 1.0.124 or higher.

The Automatic SDK supports the streaming of a [subset of ELM 327](supported_commands) commands over a standard BluetoothSocket.  You hand us credentials, and we had you back a live socket.  There are just a few changes needed to get started:

1. Make sure you've completed [Step 3](#init_and_auth) above. Now you need to find out the BT mac address of the adapter.
    - You can ask the Automatic core app for the mac address of the currently connected adapter through service binding. Please refer to sample app for more details:

        ```java
        // bind to the Automatic core app
				Automatic.get().bindService(new ServiceBindingCallback() {
            @Override public void onBindingResponse(boolean success, SdkError sdkError) {
                if (success) {
                		// successfully bound and authenticated
										...
                };
            }
        });
				...
				...
				// once bound and authenticated
				if (Automatic.get().isServiceAuthenticated()) {
					// set callback listener
					Automatic.get().setAutomaticCoreAppQueryListener(mAutomaticCoreAppQueryListener);
					// send request to the Automatic core app through service binding
					Automatic.get().queryConnectedAdapterInfo();
				}

        ```

    - Or find adapter mac address in the Bluetooth bonded device list. Refer to `findMacFromList()` in the sample app:

        ```java
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ```
      In case the phone is paired with multiple Automatic adapters therefore multiple similar items in bonded device list. The Bluetooth naming convention of Automatic adapter is "XX-Automatic Adapter", where XX is the first two letters of the 6-char pin on the back of the adapter. Your app could display the BT bonded list to let user pick the correct one, or let user enter the 6-char pin.  

2. Call `connectElmStreaming(String mac, ELMSocket.ElmSocketCallback elmSocketCallback)` with adapter mac address and implement `ELMSocket.ElmSocketCallback()` to receive the ELMSocket. ELMSocket handles I/O with `sendCommand()` and a `CommandCallback`.

     ```java
     Automatic.get().connectElmStreaming(mAdapterMac, new ELMSocket.ElmSocketCallback() {
                @Override
                public void onSocketAuthenticated(ELMSocket elmSocket) {
                    Log.v(TAG, "ELM Socket connected!";
                    mElmSocket = socket;
                    // Register ELM command callback
                    mElmSocket.setCommandCallback(mCommandCallback);
                    // Now you can send ELM commands to the adapter
                    mElmSocket.sendCommand("ELM command")
                }
                @Override
                public void onFailure(SdkError error) {
                    // failure.  
                }
                @Override
                public void onDisconnected(SdkError error) {
                    // try to reconnect?
                }
            });
     ```

3. If client app prefers to handle raw BT socket I/O themselves, call `connectElmStreaming(String mac, ELMSocket.ElmSocketCallback elmSocketCallback, boolean clientHandleBtSocket)` then get the raw BT socket from ELMSocket object.

    ```java
    Automatic.get().connectElmStreaming(mAdapterMac, new ELMSocket.ElmSocketCallback() {
                @Override
                public void onSocketAuthenticated(ELMSocket elmSocket) {
                    // get the raw BT socket and handle I/O in the client
                    BluetoothSocket btSocket = elmSocket.getSocket();
                    ...
                }
                ...
            }, true);
    ```

4. Disconnect when finished via `Automatic.get().disconnectElm()`

 * Currently only one *app* can be connected at a time to the ELM socket.  This means that if John's Streaming SDK App is using the socket, Joe's SDK App will fail to connect until John has disconnected.
 * Currently, only one Bluetooth connection attempt is made before failing with `Error.BT_CONNECT_FAILED`.  Because Bluetooth can be flaky, it is recommended that you retry the connection a few times before giving up.

## 5. <a name="supported_commands"></a>Supported ELM327 Commands

The Automatic Streaming SDK supports **only** a subset of the full ELM 327 command set.  The full supported command set is [available here](https://docs.google.com/document/d/1ZYFkG3V88y0ldq-r_ngGrTYpdaQoZHMr2CFco42GzD8), and a highly condensed list is available below.

- These commands work normally:

```
AT@1
ATCAF
ATD
ATE
ATH
ATI
ATL
ATS
```

- These commands are identical:

```
ATWS
ATZ (does not reboot the adapter)
```

- These commands require authentication because they return engine data:

```
ATDPN
ATRV
```

- These commands return OK, but are otherwise ignored:

```
ATCP
ATM
ATSH
ATSP0
ATSPA0
ATST
```

- OBD Commands:

```
01xx
09xx
```


Additional notes:

- Other OBD modes are not currently accessible.  We hope to add more in the future.
- Defaults are as documented in the ELM 327 data sheet.  However, developers should use `ATE0` and `ATS0` (and keep the defaults `ATCAF1`, `ATH0`, and `ATL0`) for the fastest PID rate.
- As an extension, we support the CAN multipid request syntax (e.g., `01040C0D0F101F`) on all protocols.  However, the response is always returned in CAN format, regardless of the actual OBD bus protocol (the one returned by `ATDPN`)

## 6. <a name="simulator"></a>Testing With An ECU Simulator

Testing ELM Streaming is best done with an ECU Simulator such as [this](https://www.scantool.net/ecusim-2000.html).  When testing, the following sequence and behaviors should be kept in mind:

- **Power On:** When the adapter (dongle) is plugged into power you should hear one double-beep. (ba-BEEP!).

- **Ignition On:** The adapter waits until it receives an ignition signal from the car, or until it senses significant movement. Either case will kick it into "Drive Mode", and you will hear a four-beep sound (Bip-BEEP-bip-BEEP!). This can take some time, especially in the simulator, due to protocol discovery. If you send ELM commands during protocol discovery, you may see a NO_DATA response. A good rule of thumb is to open the main Automatic app, and wait for it to say "Currently Driving" before initiating the connection to the Streaming SDK.

- **Drive Mode:** Once the adapter is in Drive Mode, it will not go into sleep mode until it receives an ignition off signal. To send an "ignition off" signal to the adapter, you can turn speed to max and RPM to zero (just remember to reset them to their normal positions afterward.) If the adapter is not in drive mode, it may enter sleep mode to save battery.

Using the above procedure, you should be able to

## 7. <a name="events"></a>Receiving Real-Time Events

The Automatic SDK offers the ability for your app to bind to the main Automatic App (if it is installed on the user's phone), and receive instantaneous events from the car such as Ignition On / Off, MIL On / Off, Location Changed, Trip Ended, etc, which would normally only be available via [Webhook](https://developer.automatic.com/api-reference/#real-time-events).  This binding is managed by a simple set of callback interfaces which you can implement anywhere in your app.  Note: You must have the proper scope in order to receive events.  If you do not have the required scope, you will not receive events.

1.  First ensure that you have Steps 2 and 3 above.  Then, register a BroadcastReceiver to filter for the action `com.automatic.ACTION_PING`, with the following code:

	```xml
	<receiver android:name="com.your.app.YourBroadcastReceiver" >
		<intent-filter>
			<action android:name="com.automatic.ACTION_PING" />
		</intent-filter>
	</receiver>
	```
This receiver allows the Automatic App to wake your app up when an event occurs, such as Ignition On.  The best practice here is to invoke a Service which then handles subsequent events.  For an example of this behavior, please see the Broadcast Receiver example within the [SDK Sample App][sample-app].

2. The Service will automatically handle binding and event pushing once you've registered the BroadcastReceiver, however if you need to bind / unbind from the service manually, you can call `Automatic.bindService()` or `Automatic.unbindService()`  You can also monitor the state of the Service Binding via a `ConnectionStateListener`.
- For example code, please see the [SDK Sample App][sample-app]

## 8. <a name="rest_api"></a>Making REST API Calls

**For complete REST API documentation, see the [Developer Documentation][api-docs] and the [SDK Sample App][sample-app]**

Use `restApi()` to make calls against the REST API, for example:

```java
	Automatic.get().restApi().getTrips(new Callback<ResultSet<Trip>>() {
        @Override
        public void success(ResultSet<Trip> trips, Response response) {
            // you got some trips!
        }

        @Override
        public void failure(RetrofitError retrofitError) {
            // handle failure
        }
    });
```

[developers]: https://developer.automatic.com
[api-docs]: https://developer.automatic.com/documentation/
[sample-app]: https://github.com/Automatic/Automatic-Android-SDK/blob/master/AutomaticSDKSampleApp.zip
