package com.deblox.docker;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class NewContainerBuilder {
    private String hostname = "";
    private String user = "";
    private Integer memory = 0;
    private Integer memorySwap = 0;
    private boolean attachedStdin = false;
    private boolean attachStdout = true;
    private boolean attachStderr = true;
    private JsonArray portSpecs = new JsonArray();
    private boolean privileged = false;
    private boolean tty = false;
    private boolean openStdin = false;
    private boolean stdinOnce = false;
    private Map<String, Object> env = new HashMap<>();
    private JsonArray cmd = new JsonArray();
    private String dns = null;
    private String image;
    private JsonObject volumes = new JsonObject();
    private String volumesFrom = "";
    private String workingDir = "";
    private JsonObject exposedPorts = new JsonObject();

    /*
    "ExposedPorts":{
                 "22/tcp": {}
         },
     */

    public NewContainerBuilder setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public NewContainerBuilder setUser(String user) {
        this.user = user;
        return this;
    }

    public NewContainerBuilder setMemory(Integer memory) {
        this.memory = memory;
        return this;
    }

    public NewContainerBuilder setMemorySwap(Integer memorySwap) {
        this.memorySwap = memorySwap;
        return this;
    }

    public NewContainerBuilder setAttachedStdin(boolean attachedStdin) {
        this.attachedStdin = attachedStdin;
        return this;
    }

    public NewContainerBuilder setAttachStdout(boolean attachStdout) {
        this.attachStdout = attachStdout;
        return this;
    }

    public NewContainerBuilder setAttachStderr(boolean attachStderr) {
        this.attachStderr = attachStderr;
        return this;
    }

    // Networking Port Exposure
    public NewContainerBuilder setPortSpecs(JsonArray portSpecs) {
        this.portSpecs = portSpecs;
        return this;
    }

    public NewContainerBuilder addPortSpec(String port) {
        this.portSpecs.addString(port);
        return this;
    }

    public NewContainerBuilder setPrivileged(boolean privileged) {
        this.privileged = privileged;
        return this;
    }

    public NewContainerBuilder setTty(boolean tty) {
        this.tty = tty;
        return this;
    }

    public NewContainerBuilder setOpenStdin(boolean openStdin) {
        this.openStdin = openStdin;
        return this;
    }

    public NewContainerBuilder setStdinOnce(boolean stdinOnce) {
        this.stdinOnce = stdinOnce;
        return this;
    }

    public NewContainerBuilder setEnv(Map<String, Object> env) {
        this.env = env;
        return this;
    }

    public NewContainerBuilder setCmd(JsonArray cmd) {
        this.cmd = cmd;
        return this;
    }

    public NewContainerBuilder setDns(String dns) {
        this.dns = dns;
        return this;
    }

    public NewContainerBuilder setImage(String image) {
        this.image = image;
        return this;
    }

    public NewContainerBuilder setVolumes(JsonObject volumes) {
        this.volumes = volumes;
        return this;
    }

    public NewContainerBuilder setVolumesFrom(String volumesFrom) {
        this.volumesFrom = volumesFrom;
        return this;
    }

    public NewContainerBuilder setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
        return this;
    }

    public NewContainer createNewContainer() {
        return new NewContainer(hostname, user, memory, memorySwap, attachedStdin, attachStdout, attachStderr, portSpecs, privileged, tty, openStdin, stdinOnce, env, cmd, dns, image, volumes, volumesFrom, workingDir);
    }

    public NewContainer createC(String image) {

        // Create the EnvMap with nothing in it
//        Map<String, Object> envmap = null;
//        envmap.put("", "");

//        Integer[] ports = new Integer[1];

        env.put("PATH", "/bin;/usr/bin");

        // Defaults if just image is specified
        NewContainer container = setHostname("")
                .setUser("")
                .setMemory(0)
                .setMemorySwap(0)
                .setAttachedStdin(false)
                .setAttachStderr(true)
                .setAttachStdout(true)
                .setPortSpecs(new JsonArray().addString("80"))
                .addPortSpec("22/udp")
                .setPrivileged(false)
                .setTty(false)
                .setOpenStdin(false)
                .setStdinOnce(false)
                .setEnv(env)
                .setCmd(new JsonArray().add("/bin/ping").add("8.8.8.8"))
                .setDns(null)
                .setImage(image)
                .setVolumes(new JsonObject())
                .setVolumesFrom("")
                .setWorkingDir("")
//                .setExposedPorts(new JsonObject().putObject("22/tcp", new JsonObject()))
                .createNewContainer();

        return container;

    }

//    public JsonObject toJson() {
//        JsonObject gen = new JsonObject()
//                .putString("Hostname", this.hostname)
//                .putString("User", this.user)
//                .putNumber("Memory", this.memory)
//                .putNumber("MemorySwap", this.memorySwap)
//                .putBoolean("AttachedStdin", this.attachedStdin)
//                .putBoolean("AttachStdout", this.attachStdout)
//                .putBoolean("AttachStderr", this.attachStderr)
//                .putArray("PortSpecs", new JsonArray(this.portSpecs))
//                .putBoolean("Privileged", this.privileged)
//                .putBoolean("Tty", this.tty)
//                .putBoolean("OpenStdin", this.openStdin)
//                .putBoolean("StdinOnce", this.stdinOnce)
//                .putObject("Env", new JsonObject(this.env))
//                .putArray("Cmd", new JsonArray(this.cmd))
//                .putString("Dns", this.dns)
//                .putString("Image", this.image)
//                .putString("Volumes", this.volumes)
//                .putString("VolumesFrom", this.volumesFrom)
//                .putString("WorkingDir", this.workingDir);
//        return gen;
//    }

}