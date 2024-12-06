/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;

@Controller
public class VideoController {
	private static final AtomicLong currentId = new AtomicLong(0L);
	private Map<Long, Video> videos = new HashMap<>();

	VideoController() {
		Long id = currentId.incrementAndGet();
		Video video = Video.create().withContentType("video/mpeg")
						.withDuration(300).withSubject("Mobile Cloud")
						.withTitle("Programming Cloud Services for ...")
						.build();
		video.setId(id);
		video.setDataUrl("http://localhost:8080/video/" + id + "/data");
		videos.put(id, video);
	}
	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "AnEmptyController"
	 * 
	 * 
	 * ________ ________ ________ ________ ___ ___ ___ ________ ___ __
	 * |\ ____\|\ __ \|\ __ \|\ ___ \ |\ \ |\ \|\ \|\ ____\|\ \|\ \
	 * \ \ \___|\ \ \|\ \ \ \|\ \ \ \_|\ \ \ \ \ \ \ \\\ \ \ \___|\ \ \/ /|_
	 * \ \ \ __\ \ \\\ \ \ \\\ \ \ \ \\ \ \ \ \ \ \ \\\ \ \ \ \ \ ___ \
	 * \ \ \|\ \ \ \\\ \ \ \\\ \ \ \_\\ \ \ \ \____\ \ \\\ \ \ \____\ \ \\ \ \
	 * \ \_______\ \_______\ \_______\ \_______\ \ \_______\ \_______\ \_______\
	 * \__\\ \__\
	 * \|_______|\|_______|\|_______|\|_______| \|_______|\|_______|\|_______|\|__|
	 * \|__|
	 * 
	 * 
	 */

	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList() {
		return videos.values();
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public @ResponseBody ResponseEntity<Video> getVideoData(@PathVariable Long id, HttpServletResponse resp) {
		if (!videos.containsKey(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "video not found");
		}
		try {
			Video v = videos.get(id);
			VideoFileManager videoFileManager = VideoFileManager.get();
			OutputStream outStream = resp.getOutputStream();
			videoFileManager.copyVideoData(v, outStream);
			outStream.flush();
			outStream.close();
			return new ResponseEntity<Video>(HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<Video>(HttpStatus.NOT_FOUND);
		}
	}

	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody() Video v) {
		Long id = currentId.incrementAndGet();
		v.setId(id);
		this.videos.put(id, v);
		return v;
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(
			@PathVariable Long id,
			@RequestBody MultipartFile videoData) throws IOException {
		if (!videos.containsKey(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "video not found");
		}
		VideoStatus videoStatus = new VideoStatus(VideoState.PROCESSING);
		VideoFileManager videoFileManager = VideoFileManager.get();
		try (InputStream inputStream = videoData.getInputStream()){
			Video video = this.videos.get(id);
			videoFileManager.saveVideoData(video, inputStream);
			this.videos.put(video.getId(), video);
			videoStatus.setState(VideoState.READY);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return videoStatus;
	}
}
