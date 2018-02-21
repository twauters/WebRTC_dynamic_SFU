VirtualHost "jitsi"
    authentication = "anonymous"
    ssl = {
        key = "/var/lib/prosody/jitsi.key";
        certificate = "/var/lib/prosody/jitsi.crt";
    }
    modules_enabled = {
        "bosh";
        "pubsub";
    }
    c2s_require_encryption = false
VirtualHost "auth.jitsi"
    authentication = "internal_plain"
admins = { "focus@auth.jitsi" }

Component "conference.jitsi" "muc"
Component "jitsi-videobridge.jitsi"
    component_secret = "YOURSECRET1"
Component "focus.jitsi"
    component_secret = "YOURSECRET2"
