package org.firstinspires.ftc.teamcode;

import android.annotation.SuppressLint;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.teamcode.drive.DriveConstants;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence;

import java.util.List;

/*
    Dragon Droids Team #19643
    RoadRunnerAutoRL
*/

@Config

@Autonomous(name="Red Warehouse", group = "drive")
public class RoadRunnerAutoRL extends LinearOpMode {
    public int[] armPositions = {
            -320, // Bottom Position
            -430, // Middle Position
            -575, // High Position
    };

    /*
        Start TensorFlow
     */

    private static final String TFOD_MODEL_ASSET = "model_ycup.tflite";

    private static final String[] LABELS = {
            "ycup",
            "Ball",
            "Cube",
            "Duck",
            "Marker"
    };

    private static final char[] POSITION_KEY = {
            'L',
            'C',
            'R',
            'N'
    };

    private static final String VUFORIA_KEY =
            "Ac5+EOX/////AAABmYZoDckxSUV7iTj4MtRwiJpvftO8hpoEtcxmCLDolUOn81SCtt0u8igYx5S9Gz9UpcHI66Vto0Wy1IhFoZ4J36MXjIwxdHCb/" +
                    "81+4+4DhbQ2tWq9Xisz4NvAu2l1IN8uj6qvmjmg02YfS7+REk+/NxVrS15d2fvFh7lE9RMlLtMwgkK9903e2mgxf48yL9IQMXoTfBJhY3" +
                    "X4cSKSzz4XGKDjqvIXnAd47NYB8TXuOwY0N8bL9+jPNPaw3E2SgOU2imUU6kCAQvrUPF24AI1FqtvlhZbeYLe/EQVJaC2fqcODw2Xp5px" +
                    "j1h4lS6tGXQqRZ1we0i4Wf/S+1/A4GDcb3B7hfpkRJP6AYvLxisBlP2qj";

    private VuforiaLocalizer vuforia;
    private TFObjectDetector tfod;

    final boolean debugMode = true;

    /*
        End TensorFlow
     */

    @Override
    public void runOpMode() throws InterruptedException {
        // Red Near Warehouse
        RoadRunnerAutoHardware drive = new RoadRunnerAutoHardware(hardwareMap);

        initVuforia();
        initTfod();
        if (tfod != null) {
            tfod.activate();
            tfod.setZoom(1.3, 16.0/9.0);
        }

        // Declare positions variables
        char positionDetected = 'L';
//        int i = 0;
//        while (!opModeIsActive()) {
//            if (i < 500) {
//                positionDetected = detectPosition();
//                telemetry.addData("Iteration", i);
//                telemetry.update();
//                sleep(10);
//                i++;
//            } else {
//                positionDetected = 'L';
//                break;
//            }
//
//        }



        telemetry.addData("Pos Detected: ", positionDetected);
        // telemetry.update();

        int index = "RCL".indexOf(positionDetected);

        waitForStart();

        if (isStopRequested()) return;

        if (index < 2) {
            index += 1;
            drive.setPoseEstimate(new Pose2d(12,61,Math.toRadians(270)));

            TrajectorySequence trajectorySequence0 = drive.trajectorySequenceBuilder(new Pose2d(12,61,Math.toRadians(270)))
                    .forward(6)
                    .turn(-Math.toRadians(150))
                    .back(15)
                    .build();

            TrajectorySequence trajectorySequence1 = drive.trajectorySequenceBuilder(trajectorySequence0.end())
                    .forward(10)
                    .build();

            TrajectorySequence trajectorySequence2 = drive.trajectorySequenceBuilder(trajectorySequence1.end())
                    .back(2 + ((index % 2) * 6.5))
                    .build();

            TrajectorySequence trajectorySequence3 = drive.trajectorySequenceBuilder(trajectorySequence1.end())
                    .turn(Math.toRadians(70))
                    .forward(70)
                    .build();


            drive.followTrajectorySequence(trajectorySequence0);

            drive.armLift.setTargetPosition(armPositions[index]);
            drive.armLift.setPower(0.75);
            while (drive.armLift.isBusy() && opModeIsActive()) {}
            DriveConstants.MAX_ACCEL = 80;
            drive.followTrajectorySequence(trajectorySequence2);

            drive.armClamp.setPosition(1.0);

            sleep(2000);

            drive.followTrajectorySequence(trajectorySequence1);

            drive.armClamp.setPosition(0.5);
            drive.armLift.setTargetPosition(0);
            drive.armLift.setPower(0.75);

            DriveConstants.MAX_ACCEL = 90;

            while (drive.armLift.isBusy() && opModeIsActive()) {}

            drive.followTrajectorySequence(trajectorySequence3);
        } else {
            TrajectorySequence trajectorySequence0 = drive.trajectorySequenceBuilder(new Pose2d(0,0, 0))
                    .forward(15)
                    .turn(-Math.toRadians(95))
                    .forward(30)
                    .turn(Math.toRadians(180))
                    .build();
            DriveConstants.MAX_ACCEL = 120;
            drive.followTrajectorySequence(trajectorySequence0);
            DriveConstants.MAX_ACCEL = 90;
        }

        telemetry.addData("Run", "Done!");
        telemetry.update();
    }

    private void initVuforia() {
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraName = hardwareMap.get(WebcamName.class, "ddcam");

        vuforia = ClassFactory.getInstance().createVuforia(parameters);
    }

    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfodParameters.minResultConfidence = 0.9f;
        tfodParameters.isModelTensorFlow2 = true;
        tfodParameters.inputSize = 320;
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABELS);
    }

    @SuppressLint("DefaultLocale")
    private char detectPosition() {
        int pos = 3;
        List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
        if (updatedRecognitions != null) {
            if (debugMode) {
                telemetry.addData("# Object Detected", updatedRecognitions.size());
            }
            // step through the list of recognitions and display boundary info.
            int i = 0;
            for (Recognition recognition : updatedRecognitions) {
                if (debugMode) {
                    telemetry.addData(String.format("label (%d)", i), recognition.getLabel());
                    telemetry.addData(String.format("  left,top (%d)", i), "%.03f , %.03f",
                            recognition.getLeft(), recognition.getTop());
                    telemetry.addData(String.format("  right,bottom (%d)", i), "%.03f , %.03f",
                            recognition.getRight(), recognition.getBottom());
                }
                i++;

                int centerX = Math.round(recognition.getLeft() + (recognition.getWidth() / 2));

                int width = 800;

                if (centerX >= 0 && centerX < width / 3) {
                    pos = 0;
                } else if (centerX >= width / 3 && centerX < 2 * width / 3) {
                    pos = 1;
                } else if (centerX >= 2 * width / 3 && centerX <= width) {
                    pos = 2;
                }
            }
            if (debugMode) {
                telemetry.update();
            }
        }

        if (debugMode) {
            telemetry.addData("Element Pos", pos);
            telemetry.update();
        }

        return POSITION_KEY[pos];
    }
}
