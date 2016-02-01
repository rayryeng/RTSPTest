RTSPTest
========

This is a library as well as a test GUI that I wrote that implements the RTSP Signalling Protocol to communicate with RTSP servers

**Author: Raymond Phan - `ray@bublcam.com`**

## Version History

* Version 1.0 - June 3rd, 2014 - Initial Creation
* Version 1.1 - August 25th, 2015 - Fix on searching for track IDs (thanks to [`@galbarm`](https://github.com/galbarm))
* Version 1.2 - February 1st, 2016 - Further fix on searching for track IDs (thanks to [`@abhijeetbhanjadeo`](https://github.com/abhijeetbhanjadeo))

# Synopsis

This library is written in Java.  There are two classes associated with this repo:

1. `RTSPControl` - The RTSP Protocol class that communicates with the server that establishes a connection, and eventually get media packets for the data file that we want.
2. `RTSPTest` - A simple Java Swing application that allows you to enter in a RTSP URL as well as push buttons that allow you to communicate with the server.

# Introduction

The Real-Time Streaming Protocol (RTSP) is a method to issue commands to a server that implements the Real-Time Protocol (RTP).  An RTP server sends data over to a receiving entity in a particular format dictated by the RTP protocol.  As such, RTSP is primarily used to communicate the wishes of the user / device, such as play, pause, teardown, setup and so on.  Once the setup is complete and a play command is issued, the server thus sends the relevant data using RTP to the device.

As such, this is code that was written that implements the RTSP protocol to communicate with servers that stream data to devices via the RTP protocol.  In order to connect to an RTSP server, the URL is in the following format:

    rtsp://address.of.server:port/pathToFileToPlay

`rtsp` signifies that this is an RTSP server we wish to communicate with.  `address.of.server` is the IP address of the RTSP server.  This can also be a DNS issued name (ex: google.com, bublcam.com, etc.) that will eventually resolve to the IP address of the server we want.  `port` is the port we wish to access on the server.  The default port for RTSP/RTP is usually 554.  `pathToFileToPlay` is the exact path of where the content you want to play is stored.  The `pathToFileToPlay` is server dependent and so you will need to double check on how this is structured before you try and access the file you want to play.  As an example, here is a valid RTSP URL at the time of this posting.  This is a RTSP test server provided by Wowza Media Systems:

    rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov

As such, the IP address is `184.72.239.149`, there is no port specified, so the default is `554`.  The video we wish to play is stored in `vod/mp4:BigBuckBunny_115k.mov`.  Bear in mind that the `:` is not a valid character to use in all operating systems in terms of directory structure.  However, Wowza has their own parsing scheme using `:` that will inevitably determine where the exact file is.  As such, it is paramount that you know how exactly to get to the media you want by knowing how to correctly structure the `pathToFileToPlay`.

# Basic RTSP Actions

There are 6 basic actions that a user may wish to send to the server, and there are other minor ones but for the sake of this README and for simplicity, we will ignore those.  These are:

* DESCRIBE
* OPTIONS
* SETUP
* PLAY
* PAUSE
* TEARDOWN

## DESCRIBE

The purpose of `DESCRIBE` is to be able to *describe* the information about the media that you are trying to stream.  This includes what codecs / protocols the audio and video is being used for the content, what kind of streaming protocols are used, and it also displays other information about you accessing the content.  This is returned in `SDP` format, or Session Description Protocol.  An example of what happens after you send a `DESCRIBE` request could look like this:

    RTSP/1.0 200 OK
    Content-Base: rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov/
    Date: Mon, 2 Jun 2014 21:06:45 UTC
    Content-Length: 587
    Session: 435048744;timeout=60
    Expires: Mon, 2 Jun 2014 21:06:45 UTC
    Cseq: 1
    Content-Type: application/sdp
    Server: Wowza Media Server 3.6.4.04 build11295
    Cache-Control: no-cache

    v=0
    o=- 435048744 435048744 IN IP4 184.72.239.149
    s=BigBuckBunny_115k.movs
    c=IN IP4 184.72.239.149
    t=0 0
    a=sdplang:en
    a=range:npt=0- 596.48
    a=control:*
    m=audio 0 RTP/AVP 96
    a=rtpmap:96 mpeg4-generic/12000/2
    a=fmtp:96 profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3;config=1490
    a=control:trackID=1
    m=video 0 RTP/AVP 97
    a=rtpmap:97 H264/90000
    a=fmtp:97 packetization-mode=1;profile-level-id=42C01E;sprop-parameter-sets=Z0LAHtkDxWhAAAADAEAAAAwDxYuS,aMuMsg==
    a=cliprect:0,0,160,240
    a=framesize:97 240-160
    a=framerate:24.0
    a=control:trackID=2

Basically, the first chunk of text gives you information about when you have accessed the data as well as some general information about it.  What is important is the very first line: `RTSP/1.0 200 OK`.  A server response code of `200` means that everything is working fine.  Anything else you should consider it as being an error.  The second chunk of text gives you information about the media you are trying to access.  What is important are the `m=video` and `m=audio` tags.  What follows each of these tags are information about the audio or video data within the content you are trying to stream.  As you can see, the video standard used for the content is H.264, while the audio is MP4.  It is also important to see that where the tags `a=control` are, this tells you the `trackID` or basically which "channel" we need to access either the audio or video data.  More information about the SDP protocol can [be found here](http://en.wikipedia.org/wiki/Session_Description_Protocol).  Another way that this can be specified is if there is a `stream` tag instead of `trackID`.  This is essentially the same thing.  There is more important information about the video and audio tracks, but the current implementation does not extract this information.   On future versions, this information will inevitably be extracted to allow for actual media playback.

## OPTIONS

This pretty much tells you what *commands* are available to you to send to the server.  When you send an `OPTIONS` command, the server will return something like this:

    RTSP/1.0 200 OK
    Supported: play.basic, con.persistent
    Cseq: 1
    Server: Wowza Media Server 3.6.4.04 build11295
    Public: DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, OPTIONS, ANNOUNCE, RECORD, GET_PARAMETER
    Cache-Control: no-cache

What is important is where the `Public:` field is.  These are all of the available commands that we can issue to the server.  As you can see, the 6 big ones that are in the list above are in this chunk of text.

## SETUP

What the `SETUP` command does is that we need to establish a connection to the server so that we can access each "channel" of data (audio and video).  As such, we need one `SETUP` command for audio and one for video.  What this may look like for audio and video that's returned from the server could be:

    RTSP/1.0 200 OK
    Date: Mon, 2 Jun 2014 23:59:19 UTC
    Transport: RTP/AVP;unicast;client_port=9000-9001;source=184.72.239.149;server_port=8224-8225;ssrc=445F8F19
    Session: 1406752451;timeout=60
    Expires: Mon, 2 Jun 2014 23:59:19 UTC
    Cseq: 2
    Server: Wowza Media Server 3.6.4.04 build11295
    Cache-Control: no-cache

    RTSP/1.0 200 OK
    Date: Mon, 2 Jun 2014 23:59:19 UTC
    Transport: RTP/AVP;unicast;client_port=9000-9001;source=184.72.239.149;server_port=8230-8231;ssrc=652A0475
    Session: 1406752451;timeout=60
    Expires: Mon, 2 Jun 2014 23:59:19 UTC
    Cseq: 3
    Server: Wowza Media Server 3.6.4.04 build11295
    Cache-Control: no-cache

Note that both of the string chunks are more or less the same.  The **only** difference is the `server_port`.

## PLAY

When we issue a `PLAY` command, this is when we start obtaining media packets.  The server could return something like this:

    RTSP/1.0 200 OK
    Range: npt=0.0-
    Session: 1456101883;timeout=60
    Cseq: 4
    RTP-Info: url=rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov/trackID=1;seq=1;rtptime=0,url=rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov/trackID=2;seq=1;rtptime=0
    Server: Wowza Media Server 3.6.4.04 build11295
    Cache-Control: no-cache

This simply displays what audio and video tracks we are trying to access.

## PAUSE

When we issue a `PAUSE` command, we want to stop the server from sending media packets, but we will eventually push `PLAY` again.  When issuing a `PLAY` command after the server is paused, it should resume playback and start right where you left off.  The server could return something like this:

    RTSP/1.0 200 OK
    Session: 1456101883;timeout=60
    Cseq: 5
    Server: Wowza Media Server 3.6.4.04 build11295
    Cache-Control: no-cache

This is pretty inconsequential.  The server is just replying back with information about the current session.

## TEARDOWN

We issue a `TEARDOWN` command when we are finished with the server and wish to terminate communication.  After issuing a `TEARDOWN` command, the server could return something like:

    RTSP/1.0 200 OK
    Session: 1456101883;timeout=60
    Cseq: 6
    Server: Wowza Media Server 3.6.4.04 build11295
    Cache-Control: no-cache

This returns the same information as the `PAUSE` command.  We are essentially doing the same thing, but we are also terminating our connection with the server.

# Usual Workflow of RTSP Commands

The workflow on issuing RTSP commands to the server from start to finish is usually done in the following way:

1.  Send a `DESCRIBE` or `OPTIONS` request - This is used to either figure out which track IDs store our audio and video streams, already figuring out what kinds of commands we can issue, or querying what kind of commands we have available.
2.  If we did `OPTIONS` in Step #1, we now do `DESCRIBE`.
3.  Issue a `SETUP` command to establish what kind of "channels" we wish to stream.
4.  Issue a `PLAY` command, and we can alternate between `PAUSE` and `PLAY` at will.
5.  Issue a `TEARDOWN` command when we have finished with the server.

# How to issue RTSP Commands

To communicate with the RTSP server, you need to create specifically formatted messages.  The format is almost exactly like what the server returns to you with regards to the chunks of strings that we have seen before.  All you need to do is write out the message in its character form, then send each character in byte form (ASCII codes).  Each line of the message must have a manual carriage return (`\r`) followed by a new line (`\n`).  You first need to establish a `TCP` connection with the server over the RTSP port (default is 554).  Once you do this, you create a Datagram Socket  using any port between (0 to 65535, with the exception of 554) so that we can receive media packets from the server when the time comes (i.e. issuing the `PLAY` command) at this port.  Usually this port is at 9000 for the media data sent from the RTP server, with port 9001 to receive data via the Real-Time Control Protocol (RTCP).  This data provides feedback on the quality of service (QoS) in media distribution by periodically sending statistics information to participants in a streaming multimedia session.

Let's illustrate how these commands should be written in the same procedure as the workflow as what we have described before.

## DESCRIBE

The `DESCRIBE` command is what is usually sent first.  The way you structure the `DESCRIBE` command to be sent to the server is:

    DESCRIBE rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov RTSP/1.0\r\n
    CSeq: 1\r\n
    \r\n

The `\r\n` denote a carriage return and new line for each line.  You would actually write this into the string chunk.  The above is the **string** representation of what you would actually send to the server.  Each character would be sent as one byte (converted into ASCII) over to the server.  Bear in mind that these bytes have to be sent in big-endian (network byte order).  The Java Virtual Machine already handles this for us so there is no need to do any byte re-ordering.  Also take note of the `CSeq: 1` string.  This is known as a **Command Sequence** number, which keeps track of how many commands we have sent to the server so far.  As can be seen, this is just the first command.  For each command we send, this number needs to be incremented by 1.

When the `DESCRIBE` response is received from the server, a **Session** ID is issued.  This **Session** ID needs to be part of subsequent messages sent to the server.  This can consist of both letters and numbers.

## OPTIONS

The `OPTIONS` command looks like the following.  You can do either `DESCRIBE` or `OPTIONS` first.  Let's assume that we have sent a `DESCRIBE` command first, and let's say our Session ID that we got back was 1234567890:

    OPTIONS rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov RTSP/1.0\r\n
    CSeq: 2\r\n
    Session: 1234567890\r\n
    \r\n

## SETUP

The `SETUP` command looks like the following.  Bear in mind that we must issue a command **per media track** for setting up (i.e. one for video and one for audio).

    SETUP rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov/trackID=1 RTSP/1.0\r\n
    CSeq: 3\r\n
    Session: 1234567890\r\n
    Transport: RTP/AVP;unicast;client_port=9000-9001\r\n
    \r\n
    SETUP rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov/trackID=2 RTSP/1.0\r\n
    CSeq: 4\r\n
    Session: 1234567890\r\n
    Transport: RTP/AVP;unicast;client_port=9000-9001\r\n
    \r\n

The `Transport` field is important, as it tells the server what kind of protocol the server should be streaming with (RTP of course), as well as where we will want to receive incoming media data, specified in this part of the string (recall the Port 9000 and 9001 that was mentioned earlier).  We usually set the mode to `unicast`, signifying that the stream will only be broadcast to **one device**.  Multicast options are possible, but the server must support it.  What this means is that with one single audio/video stream, it can be replicated to many more devices than just a single one.  This is not supported by most RTSP servers, and so `unicast` is always the safest mode to use.

## PLAY

The `PLAY` command looks like this, once we have finished `SETUP`:

    PLAY rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov RTSP/1.0\r\n
    CSeq: 5\r\n
    Session: 1234567890\r\n
    \r\n

Starting to look the same isn't it?  As you can see so far, most of the commands follow the same structure.  The main difference is the command issued at the beginning of the first string.

## PAUSE

The `PAUSE` command looks like this, assuming that `PLAY` was issued before:

    PAUSE rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov RTSP/1.0\r\n
    CSeq: 6\r\n
    Session: 1234567890\r\n
    \r\n

## TEARDOWN

A `TEARDOWN` command looks like this, assuming that we are either in `PLAY` or `PAUSE`:

    TEARDOWN rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov RTSP/1.0\r\n
    CSeq: 7\r\n
    Session: 1234567890\r\n
    \r\n

Bear in mind that once you issue a `TEARDOWN` request, you need to reset the `CSeq` counter so that it starts at 1.

# How to use the library

`RTSPControl` already handles all of the intricacies that you have seen earlier.  This is encapsulated by simple method calls.  You would first create a `RTSPControl` object using one of two constructors:

1. `RTSPControl rtspControl = new RTSPControl(rtspURL);`
2. `RTSPControl rtspControl = new RTSPControl(rtspHost, rtspPort, videoFile);`

In the first method, `rtspURL` is a string that is in the format of: `rtsp://address.of.server:port/pathToFileToPlay`, like we talked about earlier.  In this case, you would do: `rtspURL = rtsp://184.72.239.149:554/vod/mp4:BigBuckBunny_115k.mov`.  In the second method, you can specify the IP/Hostname of the RTSP server, the RTSP port and the path to the video file manually.  `rtspHost` and `videoFile` are strings, while `rtspPort` is an integer.  In this case, you would do: `new RTSPControl("184.72.239.149", 554, "vod/mp4:BigBuckBunny_115k.mov);`.  By specifying `-1` as the server port, this automatically defaults to `554`.

Once you have this established, the Datagram Socket connection is all done underneath the hood.  After the constructor, you can call your standard RTSP server commands:

* `RTSPDescribe()`: Issues a `DESCRIBE` command to the server
* `RTSPOptions()`: Issues an `OPTIONS` command to the server
* `RTSPSetup()`: Issues `SETUP` commands to the server for as many tracks as there are available
* `RTSPPlay()`: Issues a `PLAY` command to the server
* `RTSPPause()`: Issues a `PAUSE` command to the server
* `RTSPTeardown()`: Issues a `TEARDOWN` command to the server

When `PLAY` is invoked, within the `RTSPControl` class, a `Timer` event gets initiated and every 20 milliseconds, media data packets are read from port 9000.  These data packets are simply stored into a `Byte` buffer and displayed on the screen in hexidecimal format.  In terms of using the actual data itself, this has not been implemented as different media protocols structure their media packets differently.  It will actually be up to you to parse through the data yourself and extract the meaningful data.

Also, as an additional feature, we keep the connection to the RTSP server alive by periodically sending an innocuous `OPTIONS` request.  This way if you try to send a command to the server, this avoids any connection timeouts.

To compile the library, simply do:

    javac RTSPControl.java

# How to use the test GUI

All you have to do is compile the source, then run the code:

    javac RTSPTest.java
    java RTSPTest

Once you run it, you will be provided with a simple interface where you can enter in a RTSP URL, as well as various buttons to select that issues the aforementioned RTSP commands.  Simply follow the workflow that has been laid out previously.  When you run the RTSP test GUI, the text field will already be populated with the RTSP URL to the test Wowza server.  In order to create a connection to the server, you need to push ENTER when the text field has focus, **even with the default text in the field**.  When you eventually get to the `PLAY` command, you will see the bytes written to the screen as well as how many bytes were written at each reading of the port where the media data packets are coming in.

# References

This link was an invaluable resource: http://folk.uio.no/meccano/reflector/smallclient.html.  This code was also based on a very popular RTP/RTSP streaming assignment which can be found here: http://www.csee.umbc.edu/~pmundur/courses/CMSC691C/lab5-kurose-ross.html.  The full solution to this assignment can be found here: https://github.com/sjbarag/ECE-C433/tree/master/proj3.

# Permissions

This code is protected by the MIT License.  As such, you may use this code in any products you are developing, whether it be open or closed source to your heart's content.  A copy of the license is included with this repo.  The only thing I request is that you acknowledge me if you decide to use this library for your application.  Thanks, and have fun!
