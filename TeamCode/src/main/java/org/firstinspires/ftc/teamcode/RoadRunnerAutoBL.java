package org.firstinspires.ftc.teamcode;

import android.annotation.SuppressLint;

import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
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
    RoadRunnerAutoBL
*/

@Config

@Autonomous(name="Blue Carousel", group = "drive")
public class RoadRunnerAutoBL extends LinearOpMode {
    public int[] armPositions = {
            -320, // Bottom Position
            -360, // Middle Position
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

    final boolean debugMode = false;

    /*
        End TensorFlow
     */

    @Override
    public void runOpMode() throws InterruptedException {
        // Blue Carousel
        RoadRunnerAutoHardware drive = new RoadRunnerAutoHardware(hardwareMap);

        TensorFlow tensorFlow = new TensorFlow(hardwareMap);
        // Declare positions variables
        char positionDetected = 'N';
        while (positionDetected == 'N' && (opModeIsActive() || !isStopRequested())) {
            positionDetected = tensorFlow.detectPosition(telemetry);
            sleep(10);
        }

        telemetry.addData("Pos Detected: ", positionDetected);
        telemetry.update();

        int index = "RCL".indexOf(positionDetected);

        if (isStopRequested()) return;

        waitForStart();

        sleep(1000);

        drive.setPoseEstimate(new Pose2d(-30,60,Math.toRadians(270)));

        // Turns and moves to Hub
        TrajectorySequence trajectorySequence0 = drive.trajectorySequenceBuilder(new Pose2d(-30,60,Math.toRadians(270)))
                // Gives space from wall
                .forward(6)
                // Turns to face Alliance Shipping Hub
                .turn(-Math.toRadians(140))
                // Backs into Hub
                .back(25)
                .build();

        // Moves Closer To Hub

        TrajectorySequence trajectorySequence1 = drive.trajectorySequenceBuilder(trajectorySequence0.end())
                // Backs into Hub more
                .back(5)
                .build();

        TrajectorySequence trajectorySequence2 = drive.trajectorySequenceBuilder(trajectorySequence1.end())
                .forward(10)
                .build();

        TrajectorySequence trajectorySequence3 = drive.trajectorySequenceBuilder(trajectorySequence2.end())
                .forward(22)
                .turn(Math.toRadians(50))
                .forward(13)
                .build();

        TrajectorySequence trajectorySequence4 = drive.trajectorySequenceBuilder(trajectorySequence3.end())
                .back(50)
                .splineTo(new Vector2d(12,46.5), 0)
                .build();

        TrajectorySequence trajectorySequence5 = drive.trajectorySequenceBuilder(trajectorySequence4.end())
                .back(65)
                .build();

        drive.followTrajectorySequence(trajectorySequence0);

        drive.armLift.setTargetPosition(armPositions[index]);
        drive.armLift.setPower(0.75);
        while (drive.armLift.isBusy() && opModeIsActive()) {}
        drive.followTrajectorySequence(trajectorySequence1);
        if (armPositions[index] == -575) {
            drive.armClamp.setPosition(0.0);
        } else if(armPositions[index] != 0) {
            drive.armClamp.setPosition(1.0);
        }

        sleep(2000);

        drive.armClamp.setPosition(0.5);

        drive.followTrajectorySequence(trajectorySequence2);

        drive.armLift.setTargetPosition(0);
        drive.armLift.setPower(0.75);
        while (drive.armLift.isBusy() && opModeIsActive()) {}

        drive.followTrajectorySequence(trajectorySequence3);

        drive.carouselSpinner.setPower(0.65);

        sleep(2100);

        drive.carouselSpinner.setPower(0.0);

        drive.followTrajectorySequence(trajectorySequence4);

        DriveConstants.MAX_ACCEL = 110;
        drive.followTrajectorySequence(trajectorySequence5);
        DriveConstants.MAX_ACCEL = 90;

        telemetry.addData("Run", "Done!");
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
