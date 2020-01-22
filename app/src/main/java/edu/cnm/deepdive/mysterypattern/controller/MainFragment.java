package edu.cnm.deepdive.mysterypattern.controller;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import edu.cnm.deepdive.mysterypattern.R;
import edu.cnm.deepdive.mysterypattern.model.Position;
import edu.cnm.deepdive.mysterypattern.model.Terrain;
import edu.cnm.deepdive.mysterypattern.view.PatternView;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainFragment extends Fragment implements View.OnTouchListener {

  private static final int NUM_VERTICES = 3;
  private static final double JUMP_FRACTION = 0.5;

  private List<Position> vertices;
  private PatternView patternView;
  private Mode mode;
  private Terrain terrain;
  private Timer refresh;
  private Runner runner;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_main, container, false);
    patternView = view.findViewById(R.id.pattern_view);
    patternView.setOnTouchListener(this);
    vertices = new LinkedList<>();
    patternView.setVertices(vertices);
    build();
    return view;
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.options, menu);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(R.id.play).setVisible(mode == Mode.READY || mode == Mode.PAUSED);
    menu.findItem(R.id.pause).setVisible(mode == Mode.JUMPING);
    menu.findItem(R.id.reset).setVisible(mode == Mode.PAUSED);
    menu.findItem(R.id.build).setVisible(mode == Mode.PAUSED);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    boolean handled = true;
    switch (item.getItemId()) {
      case R.id.play:
        start();
        getActivity().invalidateOptionsMenu();
        break;
      case R.id.pause:
        stop();
        break;
      case R.id.reset:
        patternView.reset();
        break;
      case R.id.build:
        build();
        getActivity().invalidateOptionsMenu();
        break;
      default:
        handled = super.onOptionsItemSelected(item);
    }
    return handled;
  }

  private void stop() {
    setMode(Mode.PAUSED);
    runner = null;
    refresh.cancel();
    refresh = null;
  }

  private void start() {
    setMode(Mode.JUMPING);
    if (terrain == null) {
      terrain = new Terrain(vertices, JUMP_FRACTION);
      patternView.setTerrain(terrain);
    }
    refresh = new Timer();
    refresh.schedule(new Refresher(), 50, 50);
    runner = new Runner();
    runner.start();
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (mode == Mode.BUILDING || mode == Mode.READY) {
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
        vertices.add(new Position(event.getX(), event.getY()));
        if (vertices.size() >= NUM_VERTICES) {
          setMode(Mode.READY);
          while (vertices.size() > NUM_VERTICES) {
            vertices.remove(0);
          }
        }
        getActivity().invalidateOptionsMenu();
        patternView.invalidate();
      }
    }
    v.performClick();
    return false;
  }

  private void build() {
    Toast.makeText(getContext(), R.string.build_message, Toast.LENGTH_LONG).show();
    setMode(Mode.BUILDING);
    vertices.clear();
    patternView.reset();
    terrain = null;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
    patternView.setMode(mode);
  }

  public enum Mode {
    BUILDING, READY, JUMPING, PAUSED
  }

  private class Runner extends Thread {

    @Override
    public void run() {
      while (mode == Mode.JUMPING) {
        for (int i = 0; i < 50; i++) {
          terrain.update();
          patternView.update();
        }
        try {
          sleep(5);
        } catch (InterruptedException expected) {
          // Do nothing; get on with your life.
        }
      }
      patternView.postInvalidate();
      getActivity().invalidateOptionsMenu();
    }

  }

  private class Refresher extends TimerTask {

    @Override
    public void run() {
      patternView.postInvalidate();
    }

  }

}
