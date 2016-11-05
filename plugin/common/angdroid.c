/*
 * File: angdroid.c
 * Purpose: Native Angband port for Android platform
 *
 * Copyright (c) 2009 David Barr, Sergey N. Belinsky
 *
 * This work is free software; you can redistribute it and/or modify it
 * under the terms of either:
 *
 * a) the GNU General Public License as published by the Free Software
 *    Foundation, version 2, or
 *
 * b) the "Angband licence":
 *    This software may be copied and distributed for educational, research,
 *    and not for profit purposes provided that this copyright and statement
 *    are included in all such copies.  Other copyrights may also apply.
 */

#include <sys/time.h>
#include <unistd.h>
#include <string.h>
#include "curses.h"

#include "angband.h"
#include "init.h" // init_file_paths()
#include "game-world.h"
#include "ui-term.h"
#include "ui-init.h"
#include "ui-display.h"
#include "ui-input.h"
#include "ui-game.h" // save_game()

#ifndef BASIC_COLORS
#define BASIC_COLORS 16
#endif

static char android_files_path[1024];
static char android_savefile[50];
static int turn_save = 0;
static time_t savetime;

static u32b color_data[BASIC_COLORS];

static bool new_game = FALSE;

/*
 * Android's terms are boring
 */
typedef struct {
	term t;
} term_data;

/*
 * Number of "term_data" structures to support XXX XXX XXX
 *
 * You MUST support at least one "term_data" structure, and the
 * game will currently use up to eight "term_data" structures if
 * they are available.
 *
 * Actually, MAX_TERM_DATA is now defined as eight in 'defines.h'.
 *
 * If only one "term_data" structure is supported, then a lot of
 * the things that would normally go into a "term_data" structure
 * could be made into global variables instead.
 */
#define MAX_AND_TERM 1

/*
 * An array of "term_data" structures, one for each "sub-window"
 */
static term_data data[MAX_AND_TERM];


/*** Function hooks needed by "Term" ***/

/*
 * Init a new "term"
 *
 * This function should do whatever is necessary to prepare a new "term"
 * for use by the "z-term.c" package.  This may include clearing the window,
 * preparing the cursor, setting the font/colors, etc.  Usually, this
 * function does nothing, and the "init_and()" function does it all.
 */
static void Term_init_and(term *t)
{
	term_data *td = (term_data*)(t->data);
}

/*
 * Nuke an old "term"
 *
 * This function is called when an old "term" is no longer needed.  It should
 * do whatever is needed to clean up before the program exits, such as wiping
 * the screen, restoring the cursor, fixing the font, etc.  Often this function
 * does nothing and lets the operating system clean up when the program quits.
 */
static void Term_nuke_and(term *t)
{
}

/*
 * Do a "special thing" to the current "term"
 *
 * This function must react to a large number of possible arguments, each
 * corresponding to a different "action request" by the "z-term.c" package,
 * or by the application itself.
 *
 * The "action type" is specified by the first argument, which must be a
 * constant of the form "TERM_XTRA_*" as given in "z-term.h", and the second
 * argument specifies the "information" for that argument, if any, and will
 * vary according to the first argument.
 *
 * In general, this function should return zero if the action is successfully
 * handled, and non-zero if the action is unknown or incorrectly handled.
 */
static errr Term_xtra_and(int n, int v)
{
	term_data *td = (term_data*)(Term->data);
	jint ret;
	int key;

	/* flag to prevent re-entrant saving due to Term_xtra
	   being called in display functions during save */
	static int save_in_progress = 0;

	switch (n)
	{
		case TERM_XTRA_EVENT:
		{
			/*
			 * Process some pending events XXX XXX XXX
			 *
			 * Wait for at least one event if "v" is non-zero
			 * otherwise, if no events are ready, return at once.
			 * When "keypress" events are encountered, the "ascii"
			 * value corresponding to the key should be sent to the
			 * "Term_keypress()" function.  Certain "bizarre" keys,
			 * such as function keys or arrow keys, may send special
			 * sequences of characters, such as control-underscore,
			 * plus letters corresponding to modifier keys, plus an
			 * underscore, plus carriage return, which can be used by
			 * the main program for "macro" triggers.  This action
			 * should handle as many events as is efficiently possible
			 * but is only required to handle a single event, and then
			 * only if one is ready or "v" is true.
			 *
			 * This action is required.
			 */

			key = angdroid_getch(v);

			if (key == -1) {
				LOGD("TERM_XTRA_EVENT.saving game");

				int should_save = (turn_save != turn && turn > 1);

				if (!save_in_progress
						&& should_save
						&& player != NULL
						&& !player->is_dead) {
					save_in_progress = 1;

					save_game();

					time(&savetime);
					turn_save = turn;
					save_in_progress = 0;

					LOGD("TERM_XTRA_EVENT.saved game success");
				}
				else {
					LOGD("TERM_XTRA_EVENT.save skipped");
				}
			}
			else if (v == 0) {
				while (key != 0)
				{
					Term_keypress(key,0);
					key = angdroid_getch(v);
				}
			}
			else {
				Term_keypress(key,0);
			}

			return 0;
		}

		case TERM_XTRA_FLUSH:
		{
			flushinp();
			return 0;
		}

		case TERM_XTRA_CLEAR:
		{
			clear();
			return 0;
		}

		case TERM_XTRA_SHAPE:
		{
			curs_set(v);
			return 0;
		}

		case TERM_XTRA_FRESH:
		{
			refresh();
			return 0;
		}

		case TERM_XTRA_NOISE:
		{
			noise();
			return 0;
		}

		case TERM_XTRA_FROSH:
		case TERM_XTRA_BORED:
		case TERM_XTRA_REACT:
		case TERM_XTRA_ALIVE:
		case TERM_XTRA_LEVEL:
		{
			return 0;
		}

		case TERM_XTRA_DELAY:
		{
			/*
			 * Delay for some milliseconds XXX XXX XXX
			 */
			if (v > 0)
				usleep((unsigned int)1000 * v);

			return 0;
		}
	}

	/* Unknown or Unhandled action */
	return 1;
}


/*
 * Display the cursor
 *
 * This routine should display the cursor at the given location
 * (x,y) in some manner.  On some machines this involves actually
 * moving the physical cursor, on others it involves drawing a fake
 * cursor in some form of graphics mode.  Note the "soft_cursor"
 * flag which tells "z-term.c" to treat the "cursor" as a "visual"
 * thing and not as a "hardware" cursor.
 */
static errr Term_curs_and(int x, int y)
{
	move(y, x);
	return 0;
}


/*
 * Erase some characters
 *
 * This function should erase "n" characters starting at (x,y).
 *
 * You may assume "valid" input if the window is properly sized.
 */
static errr Term_wipe_and(int x, int y, int n)
{
	term_data *td = (term_data*)(Term->data);

	LOGD("Term_wipe_and");

	/* XXX XXX XXX */

	/* Place cursor */
	move(y, x);

	if (x + n >= td->t.wid)
		/* Clear to end of line */
		clrtoeol();
	else
		/* Clear some characters */
		hline(' ', n);

	/* Success */
	return 0;
}


/*
 * Draw some text on the screen
 */
static errr Term_text_and(int x, int y, int n, int a, const wchar_t *cp)
{
	term_data *td = (term_data*)(Term->data);

	move(y, x);
	attrset(a);
	addnwstr(n, cp);

	/* Success */
	return 0;
}

/*
 * Draw some attr/char pairs on the screen
 */
static errr Term_pict_and(int x, int y, int n, const int *ap, const wchar_t *cp,
						  const int *tap, const wchar_t *tcp)
{
	return 0;
}


/*** Internal Functions ***/


/*
 * Instantiate a "term_data" structure
 *
 * This is one way to prepare the "term_data" structures and to
 * "link" the various informational pieces together.
 *
 * This function assumes that every window should be 80x24 in size
 * (the standard size) and should be able to queue 256 characters.
 * Technically, only the "main screen window" needs to queue any
 * characters, but this method is simple.  One way to allow some
 * variation is to add fields to the "term_data" structure listing
 * parameters for that window, initialize them in the "init_and()"
 * function, and then use them in the code below.
 *
 * Note that "activation" calls the "Term_init_and()" hook for
 * the "term" structure, if needed.
 */
static void term_data_link(int i)
{
	term_data *td = &data[i];
	term *t = &td->t;

	/* Initialize the term */
	term_init(t, 80, 24, 256);

#if defined(ANGDROID_ANGBAND_PLUGIN)
	t->complex_input = TRUE;
#endif

	/* Choose "soft" or "hard" cursor XXX XXX XXX */
	/* A "soft" cursor must be explicitly "drawn" by the program */
	/* while a "hard" cursor has some "physical" existance and is */
	/* moved whenever text is drawn on the screen.  See "z-term.c". */
	t->soft_cursor = FALSE;

	/* Use "Term_text()" even for "black" text XXX XXX XXX */
	/* See the "Term_text_and()" function above. */
	t->always_text = TRUE;

	/* Ignore the "TERM_XTRA_BORED" action XXX XXX XXX */
	/* This may make things slightly more efficient. */
	 t->never_bored = TRUE;

	/* Ignore the "TERM_XTRA_FROSH" action XXX XXX XXX */
	/* This may make things slightly more efficient. */
	t->never_frosh = TRUE;

	/* Erase with "white space" XXX XXX XXX */
	/* t->attr_blank = TERM_WHITE; */
	/* t->char_blank = ' '; */

	/* Prepare the init/nuke hooks */
	t->init_hook = Term_init_and;
	t->nuke_hook = Term_nuke_and;

	/* Prepare the template hooks */
	t->xtra_hook = Term_xtra_and;
	t->curs_hook = Term_curs_and;
	t->wipe_hook = Term_wipe_and;
	t->text_hook = Term_text_and;
	t->pict_hook = Term_pict_and;

	/* Remember where we came from */
	t->data = td;

	/* Activate it */
	Term_activate(t);

	/* Global pointer */
	angband_term[i] = t;
}

static void hook_plog(const char* str)
{
	angdroid_warn(str);
}


/*
 * Hook to tell the user something, and then quit
 */
static void hook_quit(const char* str)
{
	LOGD("hook_quit()");
	angdroid_quit(str);
}

/*
 * Initialization function
 */
errr init_and(void)
{
	int i;

	/* Initialize globals XXX XXX XXX */

	/* Initialize "term_data" structures XXX XXX XXX */

	/* Initialize the "color_data" array XXX XXX XXX */

	/* Create windows (backwards!) */
	for (i = MAX_AND_TERM; i-- > 0; )
	{
		/* Link */
		term_data_link(i);
	}

	/* Success */
	return 0;
}


/*
 * Init some stuff
 *
 * This function is used to keep the "path" variable off the stack.
 */

void init_android_stuff(void)
{
	LOGD("angdroid.init_android_stuff");

	/* Prepare the path XXX XXX XXX */
	/* This must in some way prepare the "path" variable */
	/* so that it points at the "lib" directory.  Every */
	/* machine handles this in a different way... */
	// passed in android_files_path;

	/* Make sure it's terminated */
	android_files_path[511] = '\0';

	/* Hack -- Add a path separator (only if needed) */
	if (!suffix(android_files_path, PATH_SEP)) my_strcat(android_files_path, PATH_SEP, sizeof(android_files_path));

	//LOGD(android_files_path);

	/* Prepare the filepaths */
	init_file_paths(android_files_path, android_files_path, android_files_path);
	if (!file_exists(android_files_path)) {
		/* Warning */
		plog_fmt("Unable to open the '%s' file.", android_files_path);
		quit("The Angband 'lib' folder is probably missing or misplaced.");
	}

	char temp[1024];
	strnfmt(temp, sizeof(temp), "%s", android_savefile);
	path_build(savefile, sizeof(savefile), ANGBAND_DIR_SAVE, temp);

	savefile_set_name(android_savefile);
}

int queryInt(const char* argv0) {
	int result = -1;
	if (strcmp(argv0,"pv")==0) {
		result = 1;
	}
	else if (strcmp(argv0,"px")==0) {
		result = player->px;
	}
	else if (strcmp(argv0,"rl")==0) {
		result = 0;
		if (op_ptr && OPT(rogue_like_commands)) result=1;
	}
	else {
		result = -1; //unknown command
	}
	return result;
}

void angdroid_process_argv(int i, const char* argv)
{
	switch(i) {
		case 0: //files path
			my_strcpy(android_files_path, argv, strlen(argv)+1);
			break;
		case 1: //savefile
			my_strcpy(android_savefile, argv, strlen(argv)+1);
			break;
		default:
			break;
	}
}

void angdroid_main()
{
	/* Initialize the curses colors to our own RGB definitions */
	int i;
	for (i = 0; i < BASIC_COLORS; i++) {
		color_data[i] = ((u32b)(0xFF << 24))
			| ((u32b)(angband_color_table[i][1] << 16))
			| ((u32b)(angband_color_table[i][2] << 8))
			| ((u32b)(angband_color_table[i][3]));
	}

	initscr();
	for (i = 0; i < BASIC_COLORS; i++) {
		init_color(i, color_data[i]);
	}
	for (i = 0; i < BASIC_COLORS; i++) {
		init_pair(i, i, 0);
	}

	plog_aux = hook_plog;
	quit_aux = hook_quit;

	if (init_and() != 0) quit("Oops!");

	/* XXX XXX XXX */
	ANGBAND_SYS = "and";

	create_needed_dirs();

	/* Initialize some stuff */
	init_android_stuff();

	cmd_get_hook = textui_get_cmd;

	init_display();
	init_angband();
	textui_init();

	/* Wait for response */
	pause_line(Term);

	/* Play game */
	time(&savetime);
	play_game(false);

	/* Free resources */
	textui_cleanup();
	cleanup_angband();

	quit(NULL);
}
