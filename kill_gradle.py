import os
import signal
import subprocess

p = subprocess.Popen(["ps", "aux"], stdout=subprocess.PIPE)
out, err = p.communicate()
for line in out.decode("utf-8").split("\n"):
    if "gradle" in line and "java" in line:
        pid = int(line.split()[1])
        try:
            os.kill(pid, signal.SIGTERM)
        except:
            pass
