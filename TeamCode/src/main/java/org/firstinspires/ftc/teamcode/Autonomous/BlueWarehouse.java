package org.firstinspires.ftc.teamcode.Autonomous;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.HandlerThread;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.arcrobotics.ftclib.geometry.Rotation2d;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.Rotation;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;
import org.firstinspires.ftc.robotcore.internal.system.RefCounted;
import org.firstinspires.ftc.teamcode.Drive;
import org.firstinspires.ftc.teamcode.dev.TensorFlow;
import org.firstinspires.ftc.teamcode.drive.DriveConstants;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence;
import org.firstinspires.ftc.teamcode.util.Localizers.T265Localizer;
import org.firstinspires.ftc.teamcode.util.SaveJSON;
import org.json.JSONObject;

import java.util.List;

/*
    Dragon Droids Team #19643
    BlueWarehouse
*/

@Config

@Autonomous(name = "Blue Warehouse", group = "drive")
public class BlueWarehouse extends LinearOpMode {
    public int[] armPositions = {
            -320, // Bottom Position
            -400, // Middle Position
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
        // Blue Near Warehouse
        Drive drive = new Drive(hardwareMap, true);

        TensorFlow tensorflow = new TensorFlow(hardwareMap);

        // Declare positions variables
        char positionDetected = 'L';
        while (!opModeIsActive()) {
            positionDetected = tensorflow.detectPosition(telemetry);
            positionDetected = positionDetected == 'N' ? 'L' : positionDetected;
            sleep(10);
            telemetry.addData("Pos Detected: ", positionDetected);
            telemetry.update();
        }

        telemetry.addData("Pos Detected: ", positionDetected);
        telemetry.update();

        int index = "RCL".indexOf(positionDetected);

        waitForStart();

        if (isStopRequested()) return;

        sleep(500);

        drive.t265Localizer.setPoseEstimate(new Pose2d(12,61, Math.toRadians(90)));
//        T265Localizer.slamra.setPose(new com.arcrobotics.ftclib.geometry.Pose2d(12,61, new Rotation2d(Math.toRadians(0))));
//        drive.setPoseEstimate(new Pose2d(0,0, Math.toRadians(270)));
//        drive.update();
        drive.updatePoseEstimate();

        telemetry.addData("Pose Estimate", drive.getPoseEstimate());
        telemetry.update();

        TrajectorySequence trajectorySequence0 = drive.trajectorySequenceBuilder(drive.getPoseEstimate())
                .forward(12)
                .turn(Math.toRadians(145))
                .back(12)
                .build();

        drive.followTrajectorySequence(trajectorySequence0);

        telemetry.addData("Pose Estimate", drive.getPoseEstimate());
        telemetry.update();

        drive.armLift.setTargetPosition(armPositions[index]);
        drive.armLift.setPower(0.75);
        while (drive.armLift.isBusy() && opModeIsActive()) {}
        DriveConstants.MAX_ACCEL = 80;
        TrajectorySequence trajectorySequence2 = drive.trajectorySequenceBuilder(drive.getPoseEstimate())
                .back(2 + ((index % 2) * 5))
                .build();

        drive.followTrajectorySequence(trajectorySequence2);

        drive.armClamp.setPosition(1.0);

        sleep(2000);

        TrajectorySequence trajectorySequence1 = drive.trajectorySequenceBuilder(drive.getPoseEstimate())
                .forward(13 - (index == 2 ? 2 : 0))
                .build();

        drive.followTrajectorySequence(trajectorySequence1);

        drive.armClamp.setPosition(0.5);
        drive.armLift.setTargetPosition(10);
        drive.armLift.setPower(0.75);

//        DriveConstants.MAX_ACCEL = 90;

        while (drive.armLift.isBusy() && opModeIsActive()) {}

        TrajectorySequence trajectorySequence3 = drive.trajectorySequenceBuilder(drive.getPoseEstimate())
                .turn(-Math.toRadians(58))
                .forward(70)
                .build();

        drive.followTrajectorySequence(trajectorySequence3);

//        TrajectorySequence full = drive.trajectorySequenceBuilder(drive.getPoseEstimate().minus(new Pose2d(0,0,Math.toRadians(0))))
//                .forward(12)
//                .turn(Math.toRadians(145))
//                .back(12)
//                .back(2 + ((index % 2) * 5))
//                .forward(13 - (index == 2 ? 2 : 0))
////                .turn(-Math.toRadians(58))
////                .forward(70)
//                .build();
//
//        drive.followTrajectorySequence(full);
//        Sad attempt numero uno... :(

//        JSONObject position = new JSONObject();
//
//        try {
//            position.put("x", drive.getPoseEstimate().getX());
//            position.put("y", drive.getPoseEstimate().getY());
//            position.put("heading", drive.getPoseEstimate().getHeading());
//            new SaveJSON("position.json", position.toString());
//            telemetry.addData("Position", "Succeeded");
//        } catch (Exception e) {
//            telemetry.addData("Position", "Failed");
//        }
//        telemetry.update();

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

                int border = 20;
                int width = recognition.getImageWidth() - 2*border;

                if (centerX >= border && centerX < width / 3 + border) {
                    pos = 0;
                } else if (centerX >= width / 3 + border && centerX < 2 * width / 3 + border) {
                    pos = 1;
                } else if (centerX >= 2 * width / 3 + border && centerX <= width + border) {
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
