<?php

#echo "now loading";

chdir('/var/www/html');
system("rm output.jpg");
system("y");
system("cd ..");
system("cd ..");
system("cd ..");

chdir('/home/team63/darknet');

system("rm predictions.jpg");
system("y");
system("./darknet detector test data/obj.data class_81_again.cfg backup/class_81_again_final.weights data/new_file.jpg");

echo "<script>
        location.replace('./tts_test.html');
      </script>";
?>
