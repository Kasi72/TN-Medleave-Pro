import { describe, it, expect } from 'vitest';
import { generateLeavePlan } from './plannerEngine';

describe('plannerEngine', () => {
    it('prioritizes a 1-day sandwich bridge over isolated days', () => {
        // 2026-12-25 (Fri) is Christmas, 2026-12-27 (Sun) is Sunday -> 2026-12-26 (Sat) is a 1-day sandwich gap
        const blocks = generateLeavePlan({
            startDateStr: '2026-12-21',
            balances: { cl: 1, rh: 0, comp: 0, el: 0 },
            customHolidays: ['2026-12-25'],
            holidayDetails: { '2026-12-25': 'Christmas' },
        });

        expect(blocks).toHaveLength(1);
        expect(blocks[0].plannedLeaves[0]).toMatchObject({
            date: '2026-12-26',
            type: 'CL',
        });
        expect(blocks[0].totalDays).toBe(3); // Dec 25, 26, 27
    });

    it('respects blockedDates and pinnedDates', () => {
        const blocks = generateLeavePlan({
            startDateStr: '2026-12-21',
            balances: { cl: 2, rh: 0, comp: 0, el: 0 },
            customHolidays: ['2026-12-25'],
            blockedDates: new Set(['2026-12-26']),
            pinnedDates: { '2026-12-24': 'AUTO' },
        });

        const plannedDates = blocks.flatMap((b) => b.plannedLeaves.map((l) => l.date));
        expect(plannedDates).not.toContain('2026-12-26');
        expect(plannedDates).toContain('2026-12-24');
    });

    it('bounds Comp Off usage to credit expiryDate', () => {
        const blocks = generateLeavePlan({
            startDateStr: '2026-12-01',
            balances: { cl: 0, rh: 0, comp: 1, el: 0 },
            compCredits: [
                {
                    id: 'credit-1',
                    date: '2026-06-15',
                    remainingDays: 1,
                    expiryDate: '2026-12-12',
                    isExpired: false,
                },
            ],
        });

        expect(blocks).toHaveLength(1);
        const compLeave = blocks[0].plannedLeaves[0];
        expect(compLeave.type).toBe('Compensatory_Used');
        expect(compLeave.linkedCreditId).toBe('credit-1');
        expect(compLeave.date <= '2026-12-12').toBe(true);
    });

    it('creates virtual Comp credits from plannedToWorkHolidays', () => {
        const blocks = generateLeavePlan({
            startDateStr: '2026-12-21',
            balances: { cl: 0, rh: 0, comp: 1, el: 0 },
            customHolidays: ['2026-12-25'],
            plannedToWorkHolidays: new Set(['2026-12-27']), // Work on Sunday -> earns virtual Comp Off
        });

        const compLeaves = blocks.flatMap((b) =>
            b.plannedLeaves.filter((l) => l.type === 'Compensatory_Used')
        );
        expect(compLeaves).toHaveLength(1);
        expect(compLeaves[0].linkedCreditId).toBe('virtual-credit-2026-12-27');
    });
});
