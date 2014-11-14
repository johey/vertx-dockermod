package com.deblox.docker;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: keghol
 * Date: 12/20/13
 * Time: 11:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class NewContainer {
    final String Hostname;
    final String User;
    final Integer Memory;
    final Integer MemorySwap;
    final boolean AttachedStdin;
    final boolean AttachStdout;
    final boolean AttachStderr;
    final JsonArray PortSpecs;
    final boolean Privileged;
    final boolean Tty;
    final boolean OpenStdin;
    final boolean StdinOnce;
    final Map<String, Object> Env;
    final JsonArray Cmd;
    final String Dns;
    final String Image;
    final JsonObject Volumes;
    final String VolumesFrom;
    final String WorkingDir;
//    final JsonObject ExposedPorts;

    public NewContainer(String hostname, String user, Integer memory, Integer memorySwap, boolean attachedStdin, boolean attachStdout, boolean attachStderr, JsonArray portSpecs, boolean privileged, boolean tty, boolean openStdin, boolean stdinOnce, Map<String, Object> env, JsonArray cmd, String dns, String image, JsonObject volumes, String volumesFrom, String workingDir) {

        this.Hostname = hostname;
        this.User = user;
        this.Memory = memory;
        this.MemorySwap = memorySwap;
        this.AttachedStdin = attachedStdin;
        this.AttachStdout = attachStdout;
        this.AttachStderr = attachStderr;
        this.PortSpecs = portSpecs;
        this.Privileged = privileged;
        this.Tty = tty;
        this.OpenStdin = openStdin;
        this.StdinOnce = stdinOnce;
        this.Env = env;
        this.Cmd = cmd;
        this.Dns = dns;
        this.Image = image;
        this.Volumes = volumes;
        this.VolumesFrom = volumesFrom;
        this.WorkingDir = workingDir;
//        this.ExposedPorts = exposedPorts;
    }

    // rput everything into the a json object and return it
    public JsonObject toJson() {
        JsonObject gen = new JsonObject();
        gen.putString("Hostname", this.Hostname);
        gen.putString("User", this.User);
        gen.putNumber("Memory", this.Memory);
        gen.putNumber("MemorySwap", this.MemorySwap);
        gen.putBoolean("AttachedStdin", this.AttachedStdin);
        gen.putBoolean("AttachStdout", this.AttachStdout);
        gen.putBoolean("AttachStderr", this.AttachStderr);
//        gen.putObject("PortSpecs", null);
        gen.putArray("PortSpecs", this.PortSpecs);
        gen.putBoolean("Privileged", this.Privileged);
        gen.putBoolean("Tty", this.Tty);
        gen.putBoolean("OpenStdin", this.OpenStdin);
        gen.putBoolean("StdinOnce", this.StdinOnce);
        // cant get Env to work, so nulling for now...
        gen.putObject("Env", null);
        gen.putArray("Cmd", this.Cmd);
        gen.putString("Dns", this.Dns);
        gen.putString("Image", this.Image);
        gen.putObject("Volumes", this.Volumes);
        gen.putString("VolumesFrom", this.VolumesFrom);
        gen.putString("WorkingDir", this.WorkingDir);
//        gen.putObject("ExposedPorts", this.ExposedPorts);
        return gen;
    }
}
