import { Injectable } from '@angular/core';
import { MediacenterV1Service } from '../api/services/mediacenter-v-1.service';

@Injectable({
    providedIn: 'root',
})
export class MediacenterService extends MediacenterV1Service {}
