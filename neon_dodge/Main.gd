extends Node2D

const W := 720.0
const H := 1280.0
const PLAYER_Y := 1080.0
const PLAYER_R := 34.0

var player_x := W / 2.0
var target_x := W / 2.0
var score := 0.0
var best := 0
var lives := 3
var game_over := false
var paused := false
var spawn_timer := 0.0
var spawn_interval := 0.85
var speed_base := 340.0
var obstacles: Array = []
var particles: Array = []
var stars: Array = []
var rng := RandomNumberGenerator.new()

func _ready() -> void:
	rng.randomize()
	load_best()
	for i in 90:
		stars.append(Vector2(rng.randf_range(0, W), rng.randf_range(0, H)))
	set_process(true)
	queue_redraw()

func _process(delta: float) -> void:
	if paused:
		queue_redraw()
		return
	if not game_over:
		score += delta * 12.0
		player_x = lerp(player_x, target_x, min(1.0, delta * 12.0))
		spawn_timer -= delta
		var difficulty := min(2.5, 1.0 + score / 600.0)
		spawn_interval = max(0.28, 0.85 / difficulty)
		if spawn_timer <= 0.0:
			spawn_obstacle(difficulty)
			spawn_timer = spawn_interval
		update_obstacles(delta, difficulty)
	update_particles(delta)
	queue_redraw()

func spawn_obstacle(difficulty: float) -> void:
	var r := rng.randf_range(24.0, 54.0)
	obstacles.append({
		"p": Vector2(rng.randf_range(r, W - r), -70.0),
		"r": r,
		"speed": speed_base * rng.randf_range(0.85, 1.25) * difficulty,
		"phase": rng.randf_range(0.0, TAU),
		"drift": rng.randf_range(-55.0, 55.0)
	})

func update_obstacles(delta: float, difficulty: float) -> void:
	for i in range(obstacles.size() - 1, -1, -1):
		var o = obstacles[i]
		o.p.y += o.speed * delta
		o.phase += delta * 2.0
		o.p.x += sin(o.phase) * o.drift * delta
		if o.p.distance_to(Vector2(player_x, PLAYER_Y)) < o.r + PLAYER_R - 8.0:
			burst(Vector2(player_x, PLAYER_Y))
			obstacles.remove_at(i)
			lives -= 1
			if lives <= 0:
				end_game()
			continue
		if o.p.y > H + 90.0:
			obstacles.remove_at(i)
		else:
			obstacles[i] = o

func burst(pos: Vector2) -> void:
	for i in 18:
		var a := rng.randf_range(0.0, TAU)
		var s := rng.randf_range(100.0, 320.0)
		particles.append({"p": pos, "v": Vector2(cos(a), sin(a)) * s, "t": 0.7})

func update_particles(delta: float) -> void:
	for i in range(particles.size() - 1, -1, -1):
		var p = particles[i]
		p.t -= delta
		p.p += p.v * delta
		p.v *= 0.96
		if p.t <= 0.0:
			particles.remove_at(i)
		else:
			particles[i] = p

func end_game() -> void:
	game_over = true
	best = max(best, int(score))
	save_best()

func restart() -> void:
	score = 0.0
	lives = 3
	game_over = false
	paused = false
	spawn_timer = 0.2
	obstacles.clear()
	particles.clear()
	player_x = W / 2.0
	target_x = player_x

func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch and event.pressed:
		var p: Vector2 = event.position * Vector2(W / get_viewport_rect().size.x, H / get_viewport_rect().size.y)
		if game_over:
			restart()
			return
		if p.y < 150.0 and p.x > W - 150.0:
			paused = not paused
			return
		target_x = clamp(p.x, PLAYER_R, W - PLAYER_R)
	elif event is InputEventScreenDrag:
		var p2: Vector2 = event.position * Vector2(W / get_viewport_rect().size.x, H / get_viewport_rect().size.y)
		target_x = clamp(p2.x, PLAYER_R, W - PLAYER_R)
	elif event is InputEventMouseButton and event.pressed:
		var pm: Vector2 = event.position * Vector2(W / get_viewport_rect().size.x, H / get_viewport_rect().size.y)
		if game_over:
			restart()
		else:
			target_x = clamp(pm.x, PLAYER_R, W - PLAYER_R)
	elif event is InputEventMouseMotion and Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT):
		var mm: Vector2 = event.position * Vector2(W / get_viewport_rect().size.x, H / get_viewport_rect().size.y)
		target_x = clamp(mm.x, PLAYER_R, W - PLAYER_R)

func _draw() -> void:
	draw_rect(Rect2(0, 0, W, H), Color("08111f"))
	for s in stars:
		var tw := 0.45 + 0.35 * sin((Time.get_ticks_msec() / 600.0) + s.x)
		draw_circle(s, 1.6, Color(0.4, 0.75, 1.0, tw))
	draw_line(Vector2(90, 0), Vector2(90, H), Color(0.0, 0.9, 1.0, 0.12), 3)
	draw_line(Vector2(W-90, 0), Vector2(W-90, H), Color(0.0, 0.9, 1.0, 0.12), 3)

	for o in obstacles:
		draw_circle(o.p, o.r + 10.0, Color(1.0, 0.15, 0.38, 0.12))
		draw_circle(o.p, o.r, Color("ff315f"))
		draw_circle(o.p + Vector2(-o.r*0.25, -o.r*0.25), o.r*0.25, Color(1,1,1,0.28))

	for p in particles:
		draw_circle(p.p, 5.0, Color(0.15, 0.95, 1.0, max(0.0, p.t)))

	var pp := Vector2(player_x, PLAYER_Y)
	draw_circle(pp, PLAYER_R + 14.0, Color(0.1, 0.95, 1.0, 0.15))
	draw_circle(pp, PLAYER_R, Color("26e6ff"))
	draw_circle(pp + Vector2(-9,-9), 8, Color(1,1,1,0.5))

	var font := ThemeDB.fallback_font
	draw_string(font, Vector2(28, 58), "SCORE  %05d" % int(score), HORIZONTAL_ALIGNMENT_LEFT, -1, 34, Color.WHITE)
	draw_string(font, Vector2(28, 102), "BEST   %05d" % best, HORIZONTAL_ALIGNMENT_LEFT, -1, 25, Color(0.65,0.8,1.0))
	draw_string(font, Vector2(W-165, 62), "❤ %d" % lives, HORIZONTAL_ALIGNMENT_LEFT, -1, 32, Color("ff6688"))
	draw_string(font, Vector2(W-92, 112), "Ⅱ" if not paused else "▶", HORIZONTAL_ALIGNMENT_CENTER, 60, 34, Color.WHITE)

	if paused:
		draw_rect(Rect2(0,0,W,H), Color(0,0,0,0.55))
		draw_string(font, Vector2(0, H/2-10), "PAUSE", HORIZONTAL_ALIGNMENT_CENTER, W, 54, Color.WHITE)
		draw_string(font, Vector2(0, H/2+45), "Tap top-right to continue", HORIZONTAL_ALIGNMENT_CENTER, W, 24, Color(0.75,0.85,1.0))

	if game_over:
		draw_rect(Rect2(0,0,W,H), Color(0,0,0,0.68))
		draw_string(font, Vector2(0, H/2-80), "GAME OVER", HORIZONTAL_ALIGNMENT_CENTER, W, 60, Color("ff5f82"))
		draw_string(font, Vector2(0, H/2-10), "Score: %d" % int(score), HORIZONTAL_ALIGNMENT_CENTER, W, 32, Color.WHITE)
		draw_string(font, Vector2(0, H/2+48), "Tap anywhere to restart", HORIZONTAL_ALIGNMENT_CENTER, W, 26, Color(0.75,0.9,1.0))

func save_best() -> void:
	var f := FileAccess.open("user://save.dat", FileAccess.WRITE)
	if f:
		f.store_32(best)

func load_best() -> void:
	if FileAccess.file_exists("user://save.dat"):
		var f := FileAccess.open("user://save.dat", FileAccess.READ)
		if f:
			best = f.get_32()
