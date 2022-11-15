import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { EduSharingApiModule } from 'ngx-edu-sharing-api';

import { SearchPageService } from './search-page.service';

describe('SearchPageService', () => {
    let service: SearchPageService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule, EduSharingApiModule.forRoot(), RouterTestingModule],
            providers: [SearchPageService],
        });
        service = TestBed.inject(SearchPageService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
